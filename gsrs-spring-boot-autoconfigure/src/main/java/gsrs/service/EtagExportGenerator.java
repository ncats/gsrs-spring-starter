package gsrs.service;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.persistence.EntityManager;

import org.springframework.hateoas.UriTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.ncats.common.yield.Yield;
import ix.core.controllers.EntityFactory;
import ix.core.models.ETag;
import ix.core.util.EntityUtils;

/**
 * An {@link ExportService} that uses a saved {@link ETag} and pulls out the original query
 * for it to download the ids of the results and then convert that into a {@link ix.core.util.EntityUtils.Key}
 * to fetch the entities.
 * @param <T>
 */
public class EtagExportGenerator<T> implements ExportGenerator<ETag,T>  {

    private static final Pattern removeTopPattern = Pattern.compile("(&top=\\d+)");
    private static final Pattern removeSkipPattern = Pattern.compile("(&skip=\\d+)");
    private static final Pattern removeViewPattern = Pattern.compile("(&view=\\s+)");


    private ObjectMapper mapper = new ObjectMapper();



    private EntityManager entityManager;

    private PlatformTransactionManager transactionManager;

    public EtagExportGenerator(EntityManager entityManager, PlatformTransactionManager transactionManager){
        this.entityManager = Objects.requireNonNull(entityManager);
        this.transactionManager = Objects.requireNonNull(transactionManager);
    }
    @Override
    public Supplier<Stream<T>> generateExportFrom(String context, ETag etag) {

        return ()-> Yield.<T>create(yieldRecipe-> {
                    System.out.println("Making export:" + etag.uri);
                    String uriToUse = etag.uri;
                    JsonNode responseAsJson;
                    JsonNode array;
                    int tries = 0;


                    do {
                        responseAsJson = makePagedSubstanceRequest(uriToUse, 0, etag.total, context);
//			System.out.println(responseAsJson);
                        JsonNode finished = responseAsJson.get("finished");
                        if (finished != null && !finished.asBoolean()) {
//				System.out.println("not finished yet... waiting");
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            //don't count this try
                            tries--;
                        }

                        array = responseAsJson.get("content");
                        //text searches and substructure searches have a content
                        //but sequence searches do not... so need to hit their results url
                        //or maybe change what url the etag saves?
                        if (array == null) {
                            JsonNode results = responseAsJson.get("results");
                            if (results != null) {
                                uriToUse = results.asText();
                            }

                        }
                        tries++;
                    } while (array == null && tries < 3);
                    //if we are here, then
                    if (array == null) {
//			System.out.println("could not fetch results!!!");
                        throw new IllegalStateException("could not fetch results");
                    }
//		System.out.println("out of while loop array =\n===============\n===============\n=============" );
//		System.out.println(array);


                    Consumer<JsonNode> arrayConsumer = a -> {
                        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                        transactionTemplate.executeWithoutResult( status->{
                        for (JsonNode sub : a) {
                            //GSRS-1760 using key view always now

                            /*
                            JSON looks like: {
                            kind:	"ix.ginas.models.v1.ChemicalSubstance"
                            idString:	"e3b22138-b251-48a4-bf67-ada0f317da4a"
                            }
                             */

                            try {
                                EntityUtils.EntityInfo ei = EntityUtils.getEntityInfoFor(sub.get("kind").asText());
                                Object _id = ei.formatIdToNative(sub.get("idString").asText());
                                EntityUtils.Key k = EntityUtils.Key.of(ei, _id);
                                Optional<EntityUtils.EntityWrapper<?>> opt = k.fetch(entityManager);
                                if (opt.isPresent()) {
                                    T value = (T) opt.get().getValue();
                                    if(value !=null){
                                        EntityFactory.EntityMapper.FULL_ENTITY_MAPPER().toJsonNode(value);
                                    }
                                    yieldRecipe.returning(value);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }


                        });
                    };

                    arrayConsumer.accept(array);

                })
                .stream();
    }



    private JsonNode makePagedSubstanceRequest(String uri, int skip, int top, String context) {

        String cleanedUri = removeTopPattern.matcher(uri).replaceAll("");
        cleanedUri = removeSkipPattern.matcher(cleanedUri).replaceAll("");
        cleanedUri = removeViewPattern.matcher(cleanedUri).replaceAll("");
        //GSRS-1760 use Key view for fast fetching to avoid paging and record edits dropping out of pagged results
        if (cleanedUri.indexOf('?') > 0) {
            //has parameters so append
            cleanedUri += "&view=key&top=" + top + "&skip=" + skip;
        } else {
            //doesn't have parameters
            cleanedUri += "?view=key&top=" + top + "&skip=" + skip;
        }
//
//        WSRequestHolder requestHolder = requestPluginCachedSupplier.get().createNewLoopbackRequestFrom(cleanedUri, request, context);

        //TODO consider using RestTemplateBuilder in configuration?
        RestTemplate restTemplate = new RestTemplate();
        
        // Tyler Peryea: This section is unforunately necessary as
        // the restTemplate itself considers all URLs provided to be not-yet percent encoded
        // so, in a rather silly turn of envents we need to UNencode the URL before sending it
        // to restTemplate to be reencoded.
        try {
            cleanedUri = URLDecoder.decode(cleanedUri.toString(), "UTF-8");
        } catch (UnsupportedEncodingException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        } // java.net class
        
        ResponseEntity<String> response = restTemplate.getForEntity(cleanedUri, String.class);

        //TODO handle errors or 404 ?
        try {
            return mapper.readTree(response.getBody());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }

    }
}
