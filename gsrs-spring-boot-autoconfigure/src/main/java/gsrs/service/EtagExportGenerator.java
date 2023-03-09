package gsrs.service;

import java.io.UnsupportedEncodingException;

import java.net.URLDecoder;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.nih.ncats.common.stream.StreamUtil;
import gov.nih.ncats.common.yield.Yield;
import gsrs.controller.hateoas.HttpRequestHolder;
import gsrs.controller.hateoas.LoopbackWebRequestHelper;
import gsrs.springUtils.GsrsSpringUtils;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.EntityFetcher;
import ix.core.controllers.EntityFactory;
import ix.core.models.ETag;
import ix.core.util.EntityUtils;
import ix.core.util.EntityUtils.EntityWrapper;
import ix.core.util.EntityUtils.Key;
import lombok.extern.slf4j.Slf4j;

/**
 * An {@link ExportService} that uses a saved {@link ETag} and pulls out the original query
 * for it to download the ids of the results and then convert that into a {@link ix.core.util.EntityUtils.Key}
 * to fetch the entities.
 * @param <T>
 */

@Slf4j
public class EtagExportGenerator<T> implements ExportGenerator<ETag,T>  {

    private static final Pattern removeTopPattern = Pattern.compile("(&top=\\d+)");
    private static final Pattern removeSkipPattern = Pattern.compile("(&skip=\\d+)");
    private static final Pattern removeViewPattern = Pattern.compile("(&view=\\s+)");

    private ObjectMapper mapper = new ObjectMapper();



    private EntityManager entityManager;

    private PlatformTransactionManager transactionManager;
    
    private HttpRequestHolder initiatingRequest;

    public EtagExportGenerator(EntityManager entityManager, PlatformTransactionManager transactionManager, HttpRequestHolder request){
        this.entityManager = Objects.requireNonNull(entityManager);
        this.transactionManager = Objects.requireNonNull(transactionManager);
        this.initiatingRequest=request;
    }
    @Override
    public Supplier<Stream<T>> generateExportFrom(String context, ETag etag) {

        return ()-> Yield.<T>create(yieldRecipe-> {
                   

                   
//		System.out.println("out of while loop array =\n===============\n===============\n=============" );
//		System.out.println(array);
        	        List<Key> keys = getKeysFromUri(etag,context);

//                    Consumer<JsonNode> arrayConsumer = a -> {
                        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
                        transactionTemplate.executeWithoutResult( status->{
                        for (Key k : keys) {
                            //GSRS-1760 using key view always now

                            /*
                            JSON looks like: {
                            kind:	"ix.ginas.models.v1.ChemicalSubstance"
                            idString:	"e3b22138-b251-48a4-bf67-ada0f317da4a"
                            }
                             */

                            try {
                               
                                Optional<EntityUtils.EntityWrapper<?>> opt = EntityFetcher.of(k).getIfPossible()
                                        .map(t->EntityWrapper.of(t));
                                if (opt.isPresent()) {
                                    T value = (T) opt.get().getValue();
                                    if(value !=null){
                                        EntityFactory.EntityMapper.FULL_ENTITY_MAPPER().toJsonNode(value);
                                    }
                                    yieldRecipe.returning(value);
                                }
                            } catch (Exception e) {
                            	log.debug(e.getMessage());                                
                            }
                        }


                        });
//                    };

//                    arrayConsumer.accept(array);

                })
                .stream();
    }

    private static Optional<Key> jsonToKey(JsonNode sub){    	
    	EntityUtils.EntityInfo ei;
		try {
			ei = EntityUtils.getEntityInfoFor(sub.get("kind").asText());
   		    Object _id = ei.formatIdToNative(sub.get("idString").asText());
		    EntityUtils.Key k = EntityUtils.Key.of(ei, _id);
		    return Optional.of(k);
		} catch (Exception e) {			
			log.debug(e.getMessage());
			return Optional.empty();			
		}        
    }
    
    public List<Key> getKeysFromUri(ETag etag, String context){
    	 String uriToUse = etag.uri;
         JsonNode responseAsJson;
         JsonNode array;
         int tries = 0;

    	
    	do {
    		responseAsJson = makePagedEntityRequest(uriToUse, 0, etag.total, context);
            JsonNode finished = responseAsJson.get("finished");
            if (finished != null && !finished.asBoolean()) {
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
            //TP: This shouldn't be the case ... if it is, there's been 
            // a misunderstanding at some point that needs to be corrected
            if (array == null) {
                JsonNode results = responseAsJson.get("results");
                if (results != null) {
                	//For async api calls, the real uri for export is found in the results json property. 
                    uriToUse = results.asText();
                }

            }
            tries++;
        } while (array == null && tries < 3);
        //if we are here, then
        if (array == null) {
//System.out.println("could not fetch results!!!");
            throw new IllegalStateException("could not fetch results");
        }
        
        return StreamUtil.forIterable(array)
        		.map(jn->jsonToKey(jn))
        		.filter(o->o.isPresent())
        		.map(o->o.get())
        		.collect(Collectors.toList());        
    	
    }
    

    private JsonNode makePagedEntityRequest(String uri, int skip, int top, String context) {
        String cleanedUri = removeTopPattern.matcher(uri).replaceAll("");
        cleanedUri = removeSkipPattern.matcher(cleanedUri).replaceAll("");
        cleanedUri = removeViewPattern.matcher(cleanedUri).replaceAll("");
        //GSRS-1760 use Key view for fast fetching to avoid paging and record edits dropping out of pagged results
        if (cleanedUri.indexOf('?') > 0) {
            //has parameters so append
            cleanedUri += "&view=key&top=" + top + "&skip=" + skip + "&fdim=0";
        } else {
            //doesn't have parameters
            cleanedUri += "?view=key&top=" + top + "&skip=" + skip + "&fdim=0";
        }

        //TODO consider using RestTemplateBuilder in configuration?
        RestTemplate restTemplate = new RestTemplate();
        
        HttpRequestHolder reqHolder = HttpRequestHolder.builder()
                .url(cleanedUri)
                .method(HttpMethod.GET)
                .build();
        
        reqHolder=StaticContextAccessor.getBean(LoopbackWebRequestHelper.class).createNewLoopbackRequestFrom(reqHolder,initiatingRequest);
        
        ResponseEntity<String> response = reqHolder.execute(restTemplate);

        //TODO handle errors or 404 ?
        try {
            return mapper.readTree(response.getBody());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }

    }
}
