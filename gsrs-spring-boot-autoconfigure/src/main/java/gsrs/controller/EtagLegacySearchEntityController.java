package gsrs.controller;


import gov.nih.ncats.common.util.Unchecked;
import gsrs.repository.ETagRepository;
import gsrs.service.EtagExportGenerator;
import gsrs.service.ExportGenerator;
import gsrs.service.ExportService;
import ix.core.models.ETag;
import ix.core.models.Principal;
import ix.core.search.SearchResult;
import ix.core.util.EntityUtils;
import ix.ginas.exporters.ExportMetaData;
import ix.ginas.exporters.ExportProcess;
import ix.ginas.exporters.Exporter;
import ix.ginas.exporters.ExporterFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.repository.core.EntityMetadata;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.function.EntityResponse;

import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class EtagLegacySearchEntityController<C extends EtagLegacySearchEntityController,  T,I> extends AbstractLegacyTextSearchGsrsEntityController<C, T,I> {

//    public EtagLegacySearchEntityController(String context, Pattern pattern) {
//        super(context, pattern);
//    }
//    public EtagLegacySearchEntityController(String context, IdHelper idHelper) {
//        super(context, idHelper);
//    }
    @Autowired
    private ETagRepository eTagRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;


    @Autowired
    private ExportService exportService;

    @Autowired
    private TaskExecutor taskExecutor;

    @Override
    protected Object createSearchResponse(List<Object> results, SearchResult result, HttpServletRequest request) {
        return saveAsEtag(results, result, request);
    }



    protected abstract Stream<T> filterStream(Stream<T> stream, Map<String, String> parameters);

    public ResponseEntity<Object> createExport(String etagId, String format, boolean publicOnly, @RequestParam("filename") String fileName, Principal prof,
                                                       @RequestParam Map<String, String> parameters) throws Exception {
        Optional<ETag> etagObj = eTagRepository.findByEtag(etagId);


        if (!etagObj.isPresent()) {
            return new ResponseEntity<>("could not find etag with Id " + etagId,gsrsControllerConfiguration.getHttpStatusFor(HttpStatus.BAD_REQUEST, parameters));
        }


        ExportMetaData emd=new ExportMetaData(etagId, etagObj.get().uri, prof, publicOnly, format);


        //Not ideal, but gets around user problem
        Stream<T> mstream = new EtagExportGenerator<T>(entityManager).generateExportFrom("substances", etagObj.get()).get();

        //GSRS-699 REALLY filter out anything that isn't public unless we are looking at private data
//        if(publicOnly){
//            mstream = mstream.filter(s-> s.getAccess().isEmpty());
//        }


        Stream<T> effectivelyFinalStream = filterStream(mstream, parameters);


        if(fileName!=null){
            emd.setDisplayFilename(fileName);
        }

        ExportProcess<T> p = exportService.createExport(emd,
                () -> effectivelyFinalStream);

        p.run(taskExecutor, out -> Unchecked.uncheck(() -> getExporterFor(format, out, parameters)));

        return new ResponseEntity<>(p.getMetaData(), HttpStatus.OK);


    }

    protected abstract ExporterFactory.Parameters createParamters(String extension, Map<String, String> parameters);

    protected abstract ExporterFactory<T> createExportFactoryFor(ExporterFactory.Parameters parameters);


    private Exporter<T> getExporterFor(String extension, OutputStream pos, Map<String, String> parameters)
            throws IOException {

        ExporterFactory.Parameters params = createParamters(extension, parameters);

        ExporterFactory<T>  factory = createExportFactoryFor(params);
        if (factory == null) {
            // TODO handle null couldn't find factory for params
            throw new IllegalArgumentException("could not find suitable factory for " + params);
        }
        return factory.createNewExporter(pos, params);
    }

    private static ETag saveAsEtag(List<Object> results, SearchResult result, HttpServletRequest request) {
        final ETag etag = new ETag.Builder()
                .fromRequest(request)
                .options(result.getOptions())
                .count(results.size())
                .total(result.getCount())

                .sha1OfRequest(request, "q", "facet")
                .build();
            //TODO add save and export support
//        if(request().queryString().get("export") ==null) {
//            etag.save();
//        }
        //content is transient so don't need to worry about transforming results
        String view = request.getParameter("view");
        if("key".equals(view)){
            etag.setContent(results.stream().map(e->  {
                Optional<EntityUtils.Key> opt = EntityUtils.EntityWrapper.of(e).getOptionalKey();
                if(opt.isPresent()){
                    return opt.get();
                }
                return e;
            }
            ).collect(Collectors.toList()));
        }else {
            etag.setContent(results);
        }
        etag.setSponosredResults(result.getSponsoredMatches());
        etag.setFacets(result.getFacets());
        etag.setFieldFacets(result.getFieldFacets());
        etag.setSelected(result.getOptions().getFacets(), result.getOptions().isSideway());


        return etag;
    }
}
