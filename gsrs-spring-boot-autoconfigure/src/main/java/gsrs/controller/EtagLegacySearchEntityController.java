package gsrs.controller;


import java.io.IOException;
import java.io.OutputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import gov.nih.ncats.common.util.Unchecked;
import gsrs.DefaultDataSourceConfig;
import gsrs.autoconfigure.GsrsExportConfiguration;
import gsrs.cache.GsrsCache;
import gsrs.controller.hateoas.GsrsLinkUtil;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModel;
import gsrs.controller.hateoas.HttpRequestHolder;
import gsrs.repository.ETagRepository;
import gsrs.service.EtagExportGenerator;
import gsrs.service.ExportService;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.ETag;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.search.SearchResultContext;
import ix.ginas.exporters.DefaultParameters;
import ix.ginas.exporters.ExportMetaData;
import ix.ginas.exporters.ExportProcess;
import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import ix.ginas.exporters.OutputFormat;
import ix.utils.CallableUtil;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
@Slf4j
public abstract class EtagLegacySearchEntityController<C extends EtagLegacySearchEntityController,  T,I> extends AbstractLegacyTextSearchGsrsEntityController<C, T,I> {

//    public EtagLegacySearchEntityController(String context, Pattern pattern) {
//        super(context, pattern);
//    }
//    public EtagLegacySearchEntityController(String context, IdHelper idHelper) {
//        super(context, idHelper);
//    }
	
	@Autowired
	GsrsCache gsrscache;
	
    @Autowired
    private ETagRepository eTagRepository;

    @PersistenceContext(unitName =  DefaultDataSourceConfig.NAME_ENTITY_MANAGER)
    private EntityManager entityManager;

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private ExportService exportService;

    @Autowired
    private TaskExecutor taskExecutor;

    @Autowired
    private GsrsExportConfiguration gsrsExportConfiguration;


    @Override
    protected Object createSearchResponse(List<Object> results, SearchResult result, HttpServletRequest request) {
        return saveAsEtag(results, result, request);
    }



    protected abstract Stream<T> filterStream(Stream<T> stream, boolean publicOnly, Map<String, String> parameters);

    /*
    GET     /$context<[a-z0-9_]+>/export                     ix.core.controllers.v1.RouteFactory.exportFormats(context: String)
GET     /$context<[a-z0-9_]+>/export/                     ix.core.controllers.v1.RouteFactory.exportFormats(context: String)
GET     /$context<[a-z0-9_]+>/export/:etagId               ix.core.controllers.v1.RouteFactory.exportOptions(context: String, etagId: String, publicOnly: Boolean ?=true)
GET     /$context<[a-z0-9_]+>/export/:etagId/               ix.core.controllers.v1.RouteFactory.exportOptions(context: String, etagId: String, publicOnly: Boolean ?=true)

GET     /$context<[a-z0-9_]+>/export/:etagId/:format               ix.core.controllers.v1.RouteFactory.createExport(context: String, etagId: String, format: String, publicOnly: Boolean ?=true)

     */
    @GetGsrsRestApiMapping("/export")
    public Object exportFormats(@RequestParam Map<String, String> parameters) throws Exception {
        return gsrsExportConfiguration.getAllSupportedFormats(getEntityService().getContext());
    }

    @GetGsrsRestApiMapping("/export/{etagId}")
    public Object exportLinks(@PathVariable("etagId") String etagId,
                              @RequestParam Map<String, String> parameters) throws Exception {
        List<ExportLink> links = new ArrayList<>();
         for(OutputFormat fmt : gsrsExportConfiguration.getAllSupportedFormats(getEntityService().getContext())){
            links.add(ExportLink.builder()
                    .displayname(fmt.getDisplayName())
                    .extension(fmt.getExtension())

                    .link(new GsrsUnwrappedEntityModel.RestUrlLink(GsrsLinkUtil.fieldLink(null,null, getLinkBuilderForEntity(getEntityService().getEntityClass()).get()
                            .slash("export").slash(etagId).slash(fmt.getExtension())
                            .withSelfRel() )
                            .getHref()))
                    .build());
        }
         return links;
    }
    @Data
    @Builder
    public static class ExportLink{
        private String displayname;
        private String extension;
        private GsrsUnwrappedEntityModel.RestUrlLink link;
    }
    

    
    @PreAuthorize("isAuthenticated()")
    @GetGsrsRestApiMapping("/export/{etagId}/{format}")
    public ResponseEntity<Object> createExport(@PathVariable("etagId") String etagId, 
                                               @PathVariable("format") String format,
                                               @RequestParam(value = "publicOnly", required = false) Boolean publicOnlyObj, 
                                               @RequestParam(value ="filename", required= false) String fileName,
                                               Principal prof,
                                               @RequestParam Map<String, String> parameters,
                                               HttpServletRequest request
                                               
            ) throws Exception {
        Optional<ETag> etagObj = eTagRepository.findByEtag(etagId);

        boolean publicOnly = publicOnlyObj==null? true: publicOnlyObj;

        if (!etagObj.isPresent()) {
            return new ResponseEntity<>("could not find etag with Id " + etagId,gsrsControllerConfiguration.getHttpStatusFor(HttpStatus.BAD_REQUEST, parameters));
        }


        ExportMetaData emd=new ExportMetaData(etagId, etagObj.get().uri, prof.getName(), publicOnly, format);


        
        //Not ideal, but gets around user problem
        Stream<T> mstream = new EtagExportGenerator<T>(entityManager, transactionManager, HttpRequestHolder.fromRequest(request))
                .generateExportFrom(getEntityService().getContext(), etagObj.get())
                .get();

        //GSRS-699 REALLY filter out anything that isn't public unless we are looking at private data
//        if(publicOnly){
//            mstream = mstream.filter(s-> s.getAccess().isEmpty());
//        }


        Stream<T> effectivelyFinalStream = filterStream(mstream, publicOnly, parameters);


        if(fileName!=null){
            emd.setDisplayFilename(fileName);
        }

        ExportProcess<T> p = exportService.createExport(emd,
                () -> effectivelyFinalStream);

        p.run(taskExecutor, out -> Unchecked.uncheck(() -> getExporterFor(format, out, publicOnly, parameters)));

        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(p.getMetaData(), parameters), HttpStatus.OK);


    }

    protected ExporterFactory.Parameters createParamters(String extension, boolean publicOnly, Map<String, String> parameters){
        for(OutputFormat f : gsrsExportConfiguration.getAllSupportedFormats(this.getEntityService().getContext())){
            if(extension.equals(f.getExtension())){
                return new DefaultParameters(f, publicOnly);
            }
        }
        throw new IllegalArgumentException("could not find supported exporter for extension '"+ extension +"'");

    }



    private Exporter<T> getExporterFor(String extension, OutputStream pos, boolean publicOnly, Map<String, String> parameters)
            throws IOException {

        ExporterFactory.Parameters params = createParamters(extension, publicOnly, parameters);

        ExporterFactory<T>  factory = gsrsExportConfiguration.getExporterFor(this.getEntityService().getContext(), params);
        if (factory == null) {
            // TODO handle null couldn't find factory for params
            throw new IllegalArgumentException("could not find suitable factory for " + params);
        }
        Exporter<T> exporter= factory.createNewExporter(pos, params);
        //autowire and proxy
        return AutowireHelper.getInstance().autowireAndProxy(exporter);
    }

    private ETag saveAsEtag(List<Object> results, SearchResult result, HttpServletRequest request) {
        final ETag etag = new ETag.Builder()
                .fromRequest(request)
                .options(result.getOptions())
                .count(results.size())
                .total(result.getCount())

                .sha1OfRequest(request, "q", "facet")
                .build();

        String view = request.getParameter("view");
        Map<String,String> viewMap;
        if(view ==null){
            viewMap = Collections.emptyMap();
        }else{
            viewMap= Collections.singletonMap("view", view);
        }
        
        
        //Due to the way that hibernate works, the series of fetches that may have happened
        //before this line could include some staged insert/update statements
        //that haven't been executed. The entities which remain attached at this time
        //may expect that they will be updated/created at the next non-readonly
        //transaction. If we clear the entityManager, this detaches the objects
        //and the updates/inserts won't happen. This shouldn't be necessary, ultimately,
        //and it's worth tracking down WHY some entities become flagged as dirty/updated
        //from simple fetches that happen before this.
        // 
        entityManager.clear();
        
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult( stauts -> {
                    if (request.getParameter("export") == null) {
                        entityManager.merge(etag);
                    }
                });
        
        //content is transient so don't need to worry about transforming results
        if(!"key".equalsIgnoreCase(viewMap.get("view"))) {
            etag.setContent(results.stream().map(r-> {
                return GsrsControllerUtil.enhanceWithView(r, viewMap);
            }).collect(Collectors.toList()));

        }else {
            etag.setContent(results);
        }
       
        
        
        etag.setSponosredResults(result.getSponsoredMatches());
        //sponsored Results also needs to be "enhanced" GSRS-2042
        if(!"key".equalsIgnoreCase(viewMap.get("view"))) {
            Object o = etag.getSponsoredResults();
            if (o != null && o instanceof List) {
                etag.setSponosredResults(((List) etag.getSponsoredResults()).stream()
                        .map(e -> GsrsControllerUtil.enhanceWithView(e, viewMap))
                        .collect(Collectors.toList()));
            }
        }
        etag.setFacets(result.getFacets());
        etag.setFieldFacets(result.getFieldFacets());
        etag.setSelected(result.getOptions().getFacets(), result.getOptions().isSideway());
//        etag.setSummary(result.getSummary());

        return etag;
    }
    static String getOrderedKey (SearchResultContext context, SearchRequest request) {
        return "fetchResult/"+context.getId() + "/" + request.getOrderedSetSha1();
    }
    static String getKey (SearchResultContext context, SearchRequest request) {
        return "fetchResult/"+context.getId() + "/" + request.getDefiningSetSha1();
    }
    
    public SearchResult getResultFor(SearchResultContext ctx, SearchRequest req, boolean preserveOrder)
            throws IOException, Exception{

        final String key = (preserveOrder)? getOrderedKey(ctx,req):getKey (ctx, req);

        CallableUtil.TypedCallable<SearchResult> tc = CallableUtil.TypedCallable.of(() -> {
            Collection results = ctx.getResults();
            SearchRequest request = new SearchRequest.Builder()
                    .subset(results)
                    .options(req.getOptions())
                    .skip(0)
                    .top(results.size())
                    .query(req.getQuery())
                    .build();
            request=instrumentSearchRequest(request);

            SearchResult searchResult =null;

            if (results.isEmpty()) {
                searchResult= SearchResult.createEmptyBuilder(req.getOptions())
                        .build();
            }else{
                //katzelda : run it through the text indexer for the facets?
//                searchResult = SearchFactory.search (request);
                searchResult = getlegacyGsrsSearchService().search(request.getQuery(), request.getOptions(), request.getSubset());
                log.debug("Cache misses: "
                        +key+" size="+results.size()
                        +" class="+searchResult);
            }

            // make an alias for the context.id to this search
            // result
            searchResult.setKey(ctx.getId());
            return searchResult;
        }, SearchResult.class);

        if(ctx.isDetermined()) {
            return gsrscache.getOrElse(key, tc);
        }else {
            return tc.call();
        }
    }

}
