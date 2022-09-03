package gsrs.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.holdingarea.model.ImportData;
import gsrs.holdingarea.model.ImportMetadata;
import gsrs.holdingarea.model.ImportRecordParameters;
import gsrs.holdingarea.model.MatchedRecordSummary;
import gsrs.holdingarea.service.HoldingAreaEntityService;
import gsrs.holdingarea.service.HoldingAreaService;
import gsrs.imports.GsrsImportAdapterFactoryFactory;
import gsrs.imports.ImportAdapterFactory;
import gsrs.imports.ImportAdapterStatistics;
import gsrs.payload.PayloadController;
import gsrs.repository.PayloadRepository;
import gsrs.security.hasAdminRole;
import gsrs.service.PayloadService;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.Payload;
import ix.core.search.text.TextIndexerFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public abstract class AbstractImportSupportingGsrsEntityController<C extends AbstractImportSupportingGsrsEntityController, T, I>
        extends AbstractLegacyTextSearchGsrsEntityController<C, T, I> {

    @Autowired
    private PayloadService payloadService;

    @Autowired
    private PayloadRepository payloadRepository;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @Autowired
    private GsrsImportAdapterFactoryFactory gsrsImportAdapterFactoryFactory;

    @Autowired
    private TextIndexerFactory textIndexerFactory;

    private CachedSupplier<List<ImportAdapterFactory<T>>> importAdapterFactories
            = CachedSupplier.of(() -> gsrsImportAdapterFactoryFactory.newFactory(this.getEntityService().getContext(),
            this.getEntityService().getEntityClass()));

    @Data
    public static class ImportTaskMetaData<T> {

        @JsonIgnore
        private UUID internalUuid;

        public String getId() {
            return internalUuid.toString();
        }

        public void setId(String newId) {
            this.internalUuid = UUID.fromString(newId);
        }

        //TODO: work on this
        private String adapter;
        private JsonNode adapterSettings;
        private JsonNode adapterSchema;

        //imported from payload
        private UUID payloadID;
        private String filename;
        private Long size;
        private String mimeType;
        private String fileEncoding;
        private String holdingAreaRecordId;

        public ImportTaskMetaData() {
        }

        public ImportTaskMetaData(String payloadID, String filename, long size, String mimeType, String id, String fileEncoding) {
            //todo: add checks for valid input
            this.payloadID = UUID.fromString(payloadID);
            this.filename = filename;
            this.size = size;
            this.mimeType = mimeType;
            this.internalUuid = UUID.fromString(id);
            this.fileEncoding = fileEncoding;
        }

        public ImportTaskMetaData copy() {
            ImportTaskMetaData task = new ImportTaskMetaData();
            task.internalUuid = this.internalUuid;
            task.payloadID = this.payloadID;
            task.size = this.size;
            task.mimeType = this.mimeType;
            task.filename = this.filename;

            task.adapter = this.adapter;
            task.adapterSettings = this.adapterSettings;
            task.adapterSchema = this.adapterSchema;
            task.fileEncoding = this.fileEncoding;

            return task;
        }

        @Override
        public String toString() {
            StringBuilder returnBuilder = new StringBuilder();
            returnBuilder.append("internalUuid: ");
            returnBuilder.append(internalUuid);
            returnBuilder.append("\n");
            returnBuilder.append("adapter: ");
            returnBuilder.append(this.adapter);
            returnBuilder.append("\n");
            returnBuilder.append("adapterSettings: ");
            returnBuilder.append(this.adapterSettings == null ? "null" : this.adapterSettings.toPrettyString());
            returnBuilder.append("\n");
            returnBuilder.append("adapterSchema: ");
            returnBuilder.append(this.adapterSchema == null ? "null" : this.adapterSchema.toPrettyString());
            returnBuilder.append("\n");
            returnBuilder.append("payloadID: ");
            returnBuilder.append(this.payloadID);
            return returnBuilder.toString();
        }

        //TODO: add _self link
    }

    public Stream<T> execute(ImportTaskMetaData<T> task) throws Exception {
        log.trace("starting in execute. task: " + task.toString());
        log.trace("using encoding {}, looking for payload with ID {}", task.fileEncoding, task.payloadID);
        return fetchAdapterFactory(task)
                .createAdapter(task.adapterSettings)
                .parse(payloadService.getPayloadAsInputStream(task.payloadID).get(), task.fileEncoding);
    }

    private ImportAdapterFactory<T> fetchAdapterFactory(ImportTaskMetaData<T> task) throws Exception {
        if (task.adapter == null) {
            throw new IOException("Cannot predict settings with null import adapter");
        }
        ImportAdapterFactory<T> adaptFac = (ImportAdapterFactory<T>)
                getImportAdapterFactory(task.adapter)
                        .orElse(null);
        if (adaptFac == null) {
            throw new IOException("Cannot predict settings with unknown import adapter:\"" + task.adapter + "\"");
        }
        log.trace("in fetchAdapterFactory, adaptFac: {}, holding area service: {}", adaptFac.getClass().getName(), adaptFac.getHoldingAreaService().getName());
        adaptFac.setFileName(task.filename);
        return adaptFac;
    }


    private HoldingAreaService getHoldingAreaService(ImportTaskMetaData<T> task) throws Exception {
        if (task.adapter == null) {
            throw new IOException("Cannot predict settings with null import adapter");
        }
        return getHoldingAreaService(task.getAdapter());
    }

    private HoldingAreaService getHoldingAreaService(String adapterName) throws Exception {
        ImportAdapterFactory<T> adaptFac =
                getImportAdapterFactory(adapterName)
                        .orElse(null);
        if (adaptFac == null) {
            throw new IOException("Cannot predict settings with unknown import adapter:\"" + adapterName + "\"");
        }
        Class<T> c = adaptFac.getHoldingAreaService();
        log.trace("in getHoldingAreaService, instantiating HoldingAreaService: {}", c.getName());
        Constructor constructor = c.getConstructor(String.class);
        Object o = constructor.newInstance(this.getEntityService().getContext());
        HoldingAreaService service = AutowireHelper.getInstance().autowireAndProxy((HoldingAreaService) o);
        if (adaptFac.getEntityServiceClass() != null) {
            Constructor entityServiceConstructor = adaptFac.getEntityServiceClass().getConstructor();
            HoldingAreaEntityService entityService = (HoldingAreaEntityService) entityServiceConstructor.newInstance();
            entityService = AutowireHelper.getInstance().autowireAndProxy(entityService);
            service.registerEntityService(entityService);
            log.trace("called registerEntityService with {}", entityService.getClass().getName());
        } else {
            log.warn("No entity service found.  Import won't get far.");
        }
        return service;
    }

    private ImportTaskMetaData<T> predictSettings(ImportTaskMetaData<T> task) throws Exception {
        log.trace("in predictSettings, task for file: {}  with payload: {}", task.getFilename(), task.payloadID);
        ImportAdapterFactory<T> adaptFac = fetchAdapterFactory(task);
        log.trace("got back adaptFac with name: {}", adaptFac.getAdapterName());
        Optional<InputStream> iStream = payloadService.getPayloadAsInputStream(task.payloadID);
        ImportAdapterStatistics predictedSettings = adaptFac.predictSettings(iStream.get());

        ImportTaskMetaData<T> newMeta = task.copy();
        newMeta.adapterSettings = predictedSettings.getAdapterSettings();
        newMeta.adapterSchema = predictedSettings.getAdapterSchema();
        return newMeta;
    }

    private Optional<Payload> fetchPayload(ImportTaskMetaData task) {
        return payloadRepository.findById(task.getPayloadID());
    }

    public ImportTaskMetaData from(Payload p) {
        ImportTaskMetaData task = new ImportTaskMetaData();
        task.internalUuid = UUID.randomUUID();
        task.payloadID = p.id;
        task.size = p.size;
        task.mimeType = p.mimeType;
        task.filename = p.name;
        return task;
    }

    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //MECHANISM TO SAVE IMPORT META DATA
    //TODO: move to better data store, perhaps more like export/download service
    private Map<UUID, ImportTaskMetaData> importTaskCache = new ConcurrentHashMap<>();

    private Optional<ImportTaskMetaData> getImportTask(UUID id) {
        return Optional.ofNullable(importTaskCache.get(id));
    }

    private Optional<ImportTaskMetaData> saveImportTask(ImportTaskMetaData importTask) {
        if (importTask.internalUuid == null) {
            importTask.internalUuid = UUID.randomUUID();
        }
        importTaskCache.put(importTask.internalUuid, importTask);
        return Optional.of(importTask);
    }
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!

    //TODO: Override in specific repos AND eventually use config parsing mechanism
    public List<ImportAdapterFactory<T>> getImportAdapters() {
        return importAdapterFactories.get();
    }

    public Optional<ImportAdapterFactory<T>> getImportAdapterFactory(String name) {
        log.trace(String.format("In getImportAdapterFactory, looking for adapter with name %s among %d", name, getImportAdapters().size()));
        if (getImportAdapters().size() > 0) {
            getImportAdapters().forEach(a -> log.trace("adapter with name: {}", a.getAdapterName()));
        }
        return getImportAdapters().stream().filter(n -> name.equals(n.getAdapterName())).findFirst();
    }


    //STEP 0: list adapter classes
    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import/adapters"})
    public ResponseEntity<Object> getImport(@RequestParam Map<String, String> queryParameters) throws IOException {
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(getImportAdapters(), queryParameters), HttpStatus.OK);
    }

    //STEP 1: UPLOAD
    @hasAdminRole
    @PostGsrsRestApiMapping("/import")
    public ResponseEntity<Object> handleImport(@RequestParam("file") MultipartFile file,
                                               @RequestParam Map<String, String> queryParameters) throws Exception {
        try {
            //This follows 3 steps:
            // 1. save the file as a payload
            // 2. save an ImportTaskMetaData that wraps the payload
            // 3. return the ImportTaskMetaData

            String adapterName = queryParameters.get("adapter");
            log.trace("handleImport, adapterName: " + adapterName);
            String fileEncoding = queryParameters.get("fileEncoding");
            log.trace("fileEncoding: " + fileEncoding);

            TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

            UUID payloadId = transactionTemplate.execute(status -> {
                try {
                    Payload payload = payloadService.createPayload(file.getOriginalFilename(),
                            PayloadController.predictMimeTypeFromFile(file),
                            file.getBytes(),
                            PayloadService.PayloadPersistType.TEMP);
                    return payload.id;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            log.trace("payloadid: " + payloadId);
            Payload payload = payloadRepository.findById(payloadId).get();
            ImportTaskMetaData itmd = from(payload);
            if (adapterName != null) {
                itmd.setAdapter(adapterName);
            }
            itmd.setFileEncoding(fileEncoding);
            if (itmd.getAdapter() != null && itmd.getAdapterSettings() == null) {
                itmd = predictSettings(itmd);
                //save after we assign the fields we'll need later on
            }
            itmd = saveImportTask(itmd).get();

            log.trace("itmd.adapterSettings: " + itmd.adapterSettings.toPrettyString());
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(itmd, queryParameters), HttpStatus.OK);
        } catch (Throwable t) {
            t.printStackTrace();
            throw t;
        }
    }

    //STEP 2: Retrieve
    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import({id})", "/import/{id}"})
    public ResponseEntity<Object> getImport(@PathVariable("id") String id,
                                            @RequestParam Map<String, String> queryParameters) throws IOException {
        Optional<ImportTaskMetaData> obj = getImportTask(UUID.fromString(id));
        if (obj.isPresent()) {
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(obj.get(), queryParameters), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }

    //STEP 2.5: Retrieve & predict if needed
    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import({id})/@predict", "/import/{id}/@predict"})
    public ResponseEntity<Object> getImportPredict(@PathVariable("id") String id,
                                                   @RequestParam Map<String, String> queryParameters) throws Exception {
        Optional<ImportTaskMetaData> obj = getImportTask(UUID.fromString(id));
        if (obj.isPresent()) {
            String adapterName = queryParameters.get("adapter");
            ImportTaskMetaData itmd = obj.get().copy();
            if (adapterName != null) {
                itmd.setAdapter(adapterName);
            }
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(predictSettings(itmd), queryParameters), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }

    //STEP 3: Configure / Update
    @hasAdminRole
    @PutGsrsRestApiMapping(value = {"/import"})
    public ResponseEntity<Object> updateImport(@RequestBody JsonNode updatedJson,
                                               @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("in updateImport");
        ObjectMapper om = new ObjectMapper();
        ImportTaskMetaData itmd = om.treeToValue(updatedJson, ImportTaskMetaData.class);

        if (itmd.getAdapter() != null && itmd.getAdapterSettings() == null) {
            itmd = predictSettings(itmd);
        }

        //TODO: validation
        //override any existing task version
        itmd = saveImportTask(itmd).get();
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(itmd, queryParameters), HttpStatus.OK);
    }

    //STEP 3.5: Preview import
    //May required additional work
    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import({id})/@preview", "/import/{id}/@preview"})
    public ResponseEntity<Object> executePreview(@PathVariable("id") String id,
                                                 @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("executePreview.  id: " + id);
        Optional<ImportTaskMetaData> obj = getImportTask(UUID.fromString(id));
        if (obj.isPresent()) {
            log.trace("retrieved ImportTaskMetaData");
            //TODO: make async and do other stuff:
            ImportTaskMetaData itmd = obj.get();

            long limit = Long.parseLong(queryParameters.getOrDefault("limit", "10"));
            log.trace("limit: {}", limit);

            List<T> previewList = (List<T>) (execute(itmd)
                    .limit(limit)
                    .collect(Collectors.toList()));

            HoldingAreaService service = getHoldingAreaService(itmd);
            ObjectMapper mapper = new ObjectMapper();
            for (T object : previewList) {
                log.trace("going to call saveHoldingAreaRecord with data of type {}", object.getClass().getName());
                log.trace(object.toString());
                saveHoldingAreaRecord(service, mapper.writeValueAsString(object), itmd);
            }

            /*log.trace("queryParameters:");
            queryParameters.keySet().forEach(k->log.trace("key: {}; value: {}", k, queryParameters.get(k)));*/
            return new ResponseEntity<>(previewList, HttpStatus.OK);
            //GsrsControllerUtil.enhanceWithView(previewList, queryParameters)
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }


    //search for records that have the same values for key fields
    @hasAdminRole
    @PostGsrsRestApiMapping(value = {"/import/matches"})
    public ResponseEntity<Object> findMatches(@RequestBody JsonNode entityJson,
                                              @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("in findMatches");
        ObjectMapper om = new ObjectMapper();
        String entityType = queryParameters.get("entityType");
        String adapterName = queryParameters.get("adapter");
        log.trace("adapterName: " + adapterName);
        log.trace("entityType: " + entityType);
        log.trace("entityJson.toString(): " + entityJson.toString());
        HoldingAreaService service = getHoldingAreaService(adapterName);
        log.trace("retrieved service");
        //findMatches
        MatchedRecordSummary summary = service.findMatchesForJson(entityType, entityJson.toString());
        Object returned;
        if (queryParameters.containsKey("view") && "full".equalsIgnoreCase(queryParameters.get("view"))) {
            returned = summary;
        } else {
            returned = summary.getMatches();
        }
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(returned, queryParameters), HttpStatus.OK);
    }

    //search for records that have the same values for key fields
    @hasAdminRole
    @DeleteGsrsRestApiMapping(value = {"/import({id})/@delete", "/import/{id}/@delete"})
    public ResponseEntity<Object> deleteRecord(@PathVariable("id") String id,
                                               @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("in deleteRecord");

        String adapterName = queryParameters.get("adapter");
        log.trace("adapterName: " + adapterName);
        if (adapterName == null || adapterName.length() == 0) {
            return new ResponseEntity<>("No adapterName supplied", HttpStatus.BAD_REQUEST);
        }
        HoldingAreaService service = getHoldingAreaService(adapterName);
        log.trace("retrieved service");
        int version = 0;//todo: retrieve from parameters
        service.deleteRecord(id, version);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    //search for records that have the same values for key fields
    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import({id})/data", "/import/{id}/data"})
    public ResponseEntity<Object> retrieveRecord(@PathVariable("id") String id,
                                                 @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("in retrieveRecord");

        String adapterName = queryParameters.get("adapter");
        log.trace("adapterName: " + adapterName);
        if (adapterName == null || adapterName.length() == 0) {
            return new ResponseEntity<>("No adapterName supplied", HttpStatus.BAD_REQUEST);
        }
        HoldingAreaService service = getHoldingAreaService(adapterName);
        log.trace("retrieved service");
        int version = Integer.parseInt(queryParameters.get("version"));
        ImportMetadata data = service.retrieveRecord(id, version);
        return new ResponseEntity<>(data, HttpStatus.OK);
    }

    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import({id})/instances", "/import/{id}/instances"})
    public ResponseEntity<Object> getInstances(@PathVariable("id") String recordId,
                                               @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("in getInstances with data");

        String adapterName = queryParameters.get("adapter");
        log.trace("adapterName: " + adapterName);
        if (adapterName == null || adapterName.length() == 0) {
            return new ResponseEntity<>("No adapterName supplied", HttpStatus.BAD_REQUEST);
        }
        HoldingAreaService service = getHoldingAreaService(adapterName);
        log.trace("retrieved service");
        List<ImportData> data = service.getDataForRecord(recordId);
        log.trace("retrieved data for record");
        return new ResponseEntity<>(data, HttpStatus.OK);
    }

    //search for records that have the same values for key fields
    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import({id})/instancedata", "/import/{id}/instancedata"})
    public ResponseEntity<Object> retrieveInstanceData(@PathVariable("id") String instanceId,
                                                       @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("in retrieveInstanceData");

        String adapterName = queryParameters.get("adapter");
        log.trace("adapterName: " + adapterName);
        if (adapterName == null || adapterName.length() == 0) {
            return new ResponseEntity<>("No adapterName supplied", HttpStatus.BAD_REQUEST);
        }
        HoldingAreaService service = getHoldingAreaService(adapterName);
        log.trace("retrieved service");
        String data = service.getInstanceData(instanceId);
        return new ResponseEntity<>(data, HttpStatus.OK);
    }

    private String saveHoldingAreaRecord(HoldingAreaService service, String json, ImportTaskMetaData importTaskMetaData) {
        ImportRecordParameters parameters = ImportRecordParameters.builder()
                .jsonData(json)
                .entityClassName("ix.ginas.models.v1.Substance")
                .formatType(importTaskMetaData.mimeType)
                //.rawData(importTaskMetaData.)
                .source(importTaskMetaData.getFilename())
                .build();
        return service.createRecord(parameters);
    }

    //STEP 3: Configure / Update
    @hasAdminRole
    @PutGsrsRestApiMapping(value = {"/import/{id}/@update", "/import({id})@update"})
    public ResponseEntity<Object> updateImportData(@PathVariable("id") String recordId,
                                                   @RequestBody String updatedJson,
                                                   @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("in updateImportData");
        String adapterName = queryParameters.get("adapter");
        log.trace("adapterName: " + adapterName);
        if (adapterName == null || adapterName.length() == 0) {
            return new ResponseEntity<>("No adapterName supplied", HttpStatus.BAD_REQUEST);
        }
        HoldingAreaService service = getHoldingAreaService(adapterName);
        log.trace("retrieved service");
        String result = service.updateRecord(recordId, updatedJson);
        ObjectNode resultNode= JsonNodeFactory.instance.objectNode();;
        resultNode.put("Results",result);

        //TODO: validation ?????
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(resultNode, queryParameters), HttpStatus.OK);
    }

    //STEP 4: Execute import
    //!!!!NEEDS MORE WORK
    @hasAdminRole
    @PostGsrsRestApiMapping(value = {"/import({id})/@execute", "/import/{id}/@execute"})
    public ResponseEntity<Object> executeImport(@PathVariable("id") String id,
                                                @RequestParam Map<String, String> queryParameters) throws Exception {
        Optional<ImportTaskMetaData> obj = getImportTask(UUID.fromString(id));
        if (obj.isPresent()) {
            //TODO: make async and do other stuff:
            ImportTaskMetaData itmd = obj.get();

            execute(itmd)
                    .forEach(t -> {
                        //TODO do something with this, likely put into some other area as a large JSON dump
                        //which will have further processing
                        System.out.println(t);
                    });

            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(itmd, queryParameters), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }

}