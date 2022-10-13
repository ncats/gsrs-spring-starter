package gsrs.controller;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.nih.ncats.common.util.Unchecked;
import gsrs.DefaultDataSourceConfig;
import gsrs.autoconfigure.GsrsExportConfiguration;
import gsrs.controller.hateoas.GsrsLinkUtil;
import gsrs.controller.hateoas.GsrsUnwrappedEntityModel;
import gsrs.controller.hateoas.HttpRequestHolder;
import gsrs.repository.ETagRepository;
import gsrs.service.EtagExportGenerator;
import gsrs.service.ExportService;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.ETag;
import ix.core.search.SearchResult;
import ix.ginas.exporters.*;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Principal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public abstract class EtagLegacySearchEntityController<C extends EtagLegacySearchEntityController,  T,I> extends AbstractExportSupportingGsrsEntityController<C, T,I> {

//    public EtagLegacySearchEntityController(String context, Pattern pattern) {
//        super(context, pattern);
//    }
//    public EtagLegacySearchEntityController(String context, IdHelper idHelper) {
//        super(context, idHelper);
//    }
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
    @GetGsrsRestApiMapping("/export") //inventory -- lists what's available.  Eventually, provide more info...
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
                                               @RequestParam(value="exportConfigId", required = false) String exportConfigId,
                                               Principal prof,
                                               @RequestParam Map<String, String> parameters,
                                               HttpServletRequest request

            ) throws Exception {
        log.warn("Starting in createExport. exportConfigId: {}", exportConfigId);
        Optional<ETag> etagObj = eTagRepository.findByEtag(etagId);

        Optional<SpecificExporterSettings> exportConfig=Optional.empty();
        if( exportConfigId == null){
            exportConfig=Optional.of(createDefaultConfig());
        } else {
            if(isInteger(exportConfigId)) {
                Long itemId=Long.parseLong(exportConfigId);
                log.trace("converted configid to {}", itemId);
                exportConfig = getConfigById(itemId);
            } else {
                Set<OutputFormat> formats= gsrsExportConfiguration.getAllSupportedFormats(getEntityService().getContext()).stream()
                        .filter(e->e.getExtension().equalsIgnoreCase(exportConfigId))
                        .collect(Collectors.toSet());
                if(formats.size()>0){
                     //
                }
                else {
                    ObjectMapper mapper= new ObjectMapper();
                    SpecificExporterSettings config = mapper.readValue(exportConfigId, SpecificExporterSettings.class);
                    if( config !=null) {
                        exportConfig=Optional.of(config);
                    }
                }
            }
        }

        if(!exportConfig.isPresent()) {
            exportConfig=Optional.of(createDefaultConfig());
        }
        Optional<SpecificExporterSettings> finalExportConfig=exportConfig;
        //instantiate all settings and scrubber...
        RecordScrubber<T> scrubber= getScrubberFactory().createScrubber(exportConfig.get().getScrubberSettings());
        log.trace("got RecordScrubber of type {}", scrubber.getClass().getName());
        RecordExpander<T> expander = getExpanderFactory(gsrsExportConfiguration.getExpanderFactory().get(getEntityService().getContext())).createExpander(exportConfig.get().getExpanderSettings());
        log.trace("got RecordExpander of type {}", expander.getClass().getName());

        boolean publicOnly = publicOnlyObj==null? true: publicOnlyObj;

        if (!etagObj.isPresent()) {
            return new ResponseEntity<>("could not find etag with Id " + etagId,gsrsControllerConfiguration.getHttpStatusFor(HttpStatus.BAD_REQUEST, parameters));
        }
        ExportMetaData emd=new ExportMetaData(etagId, etagObj.get().uri, prof.getName(), publicOnly, format);
        //Not ideal, but gets around user problem
        Stream<T> mstream = new EtagExportGenerator<T>(entityManager, transactionManager, HttpRequestHolder.fromRequest(request))
                .generateExportFrom(getEntityService().getContext(), etagObj.get())
                .get();

        Stream<T> effectivelyFinalStream = filterStream(mstream, publicOnly, parameters)
                .map(t->  scrubber.scrub(t))
                .filter(o->o.isPresent())
                .flatMap(t->expander.expandRecord(t.get()))
                .distinct()
                .map(t->  scrubber.scrub(t))
                .filter(o->o.isPresent())
                .map(o->o.get());
        log.trace("computed effectivelyFinalStream using distinct");

        if(fileName!=null){
            emd.setDisplayFilename(fileName);
        }
        ExportProcess<T> p = exportService.createExport(emd,
                () -> effectivelyFinalStream);
        p.run(taskExecutor, out -> Unchecked.uncheck(() -> getExporterFor(format, out, publicOnly, parameters, finalExportConfig.get().getExporterSettings())));
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(p.getMetaData(), parameters), HttpStatus.OK);
    }

    protected ExporterFactory.Parameters createParameters(String extension, boolean publicOnly, Map<String, String> parameters,
                                                          JsonNode detailedParameters){
        for(OutputFormat f : gsrsExportConfiguration.getAllSupportedFormats(this.getEntityService().getContext())){
            if(extension.equals(f.getExtension())){
                return new DefaultParameters(f, publicOnly, detailedParameters);
            }
        }
        throw new IllegalArgumentException("could not find supported exporter for extension '"+ extension +"'");

    }



    private Exporter<T> getExporterFor(String extension, OutputStream pos, boolean publicOnly, Map<String, String> parameters,
                                       JsonNode detailedParameters)
            throws IOException {

        ExporterFactory.Parameters params = createParameters(extension, publicOnly, parameters, detailedParameters);

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

        return etag;
    }

    public boolean isInteger(String testValue) {
        if( testValue == null || testValue.trim().length()==0){
            return false;
        }
        String testString = testValue.trim();
        Pattern integerPattern = Pattern.compile("\\d+");
        return integerPattern.matcher(testString).matches();
    }

    private SpecificExporterSettings createDefaultConfig(){
        SpecificExporterSettings config=  new SpecificExporterSettings();
        log.trace("Setting entity class to {}", getEntityService().getEntityClass().getName());
        config.setEntityClass(getEntityService().getEntityClass().getName());
        return config;
    }

    /*private CachedSupplier<List<ExporterFactoryConfig<T>>> importAdapterFactories
            = CachedSupplier.of(() -> gsrsImportAdapterFactoryFactory.newFactory(this.getEntityService().getContext(),
            this.getEntityService().getEntityClass()));*/
}
