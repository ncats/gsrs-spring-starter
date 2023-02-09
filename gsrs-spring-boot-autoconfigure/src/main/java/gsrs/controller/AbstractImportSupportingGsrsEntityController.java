package gsrs.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.dataexchange.model.ProcessingAction;
import gsrs.dataexchange.model.ProcessingActionConfig;
import gsrs.dataexchange.model.ProcessingActionConfigSet;
import gsrs.holdingarea.model.ImportData;
import gsrs.holdingarea.model.ImportMetadata;
import gsrs.holdingarea.model.ImportRecordParameters;
import gsrs.holdingarea.model.MatchedRecordSummary;
import gsrs.holdingarea.service.HoldingAreaEntityService;
import gsrs.holdingarea.service.HoldingAreaService;
import gsrs.imports.*;
import gsrs.payload.PayloadController;
import gsrs.repository.PayloadRepository;
import gsrs.security.hasAdminRole;
import gsrs.service.GsrsEntityService;
import gsrs.service.PayloadService;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.Payload;
import ix.core.search.SearchOptions;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.search.text.FacetMeta;
import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexerFactory;
import ix.core.util.EntityUtils;
import ix.core.util.pojopointer.PojoPointer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.queryparser.classic.ParseException;
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

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public abstract class AbstractImportSupportingGsrsEntityController<C extends AbstractImportSupportingGsrsEntityController, T, I>
        extends AbstractExportSupportingGsrsEntityController<C, T, I> {

    @Autowired
    public PayloadService payloadService;

    @Autowired
    private PayloadRepository payloadRepository;

    @Autowired
    protected PlatformTransactionManager platformTransactionManager;

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @Autowired
    private GsrsImportAdapterFactoryFactory gsrsImportAdapterFactoryFactory;

    @Autowired
    private TextIndexerFactory textIndexerFactory;

    private CachedSupplier<List<ImportAdapterFactory<T>>> importAdapterFactories
            = CachedSupplier.of(() -> gsrsImportAdapterFactoryFactory.newFactory(this.getEntityService().getContext(),
            this.getEntityService().getEntityClass()));

    private final static Pattern ALPHANUMERIC = Pattern.compile("^[a-zA-Z0-9-]*$");

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
        private String entityType;

        private JsonNode inputSettings;

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
            task.entityType = this.entityType;

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
            returnBuilder.append("entity type: ");
            returnBuilder.append(this.entityType);
            returnBuilder.append("\n");
            returnBuilder.append("payloadID: ");
            returnBuilder.append(this.payloadID);
            return returnBuilder.toString();
        }

        //TODO: add _self link
    }

    public Stream<T> generateObjects(ImportTaskMetaData<T> task, Map<String, String> settingsMap) throws Exception {
        log.trace("starting in execute. task: " + task.toString());
        log.trace("using encoding {}, looking for payload with ID {}", task.fileEncoding, task.payloadID);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode settingsNode = mapper.convertValue(settingsMap, ObjectNode.class);
        if (!settingsNode.hasNonNull("Encoding")) {
            settingsNode.put("Encoding", task.fileEncoding);
        }
        ImportAdapterFactory<T> factory = fetchAdapterFactory(task);
        ImportAdapter<T> adapter = factory.createAdapter(task.adapterSettings);
        Optional<InputStream> streamHolder = payloadService.getPayloadAsInputStream(task.payloadID);
        InputStream stream = streamHolder.get();
        return adapter.parse(stream, settingsNode);
    }

    protected ImportAdapterFactory<T> fetchAdapterFactory(ImportTaskMetaData<T> task) throws Exception {
        if (task.adapter == null) {
            throw new IOException("Cannot predict settings with null import adapter");
        }
        ImportAdapterFactory<T> adaptFac = getImportAdapterFactory(task.adapter)
                .orElse(null);
        if (adaptFac == null) {
            throw new IOException("Cannot predict settings with unknown import adapter:\"" + task.adapter + "\"");
        }
        log.trace("in fetchAdapterFactory, adaptFac: {}", adaptFac);
        log.trace("in fetchAdapterFactory, adaptFac: {}, ", adaptFac.getClass().getName());
        log.trace("in fetchAdapterFactory, holding area service: {}", adaptFac.getHoldingAreaService().getName());
        adaptFac.setFileName(task.filename);
        return adaptFac;
    }


    private HoldingAreaService getHoldingAreaService(ImportTaskMetaData<T> task) throws Exception {
        Objects.requireNonNull(task.adapter, "Cannot predict settings with null import adapter");
        return getHoldingAreaService(task.getAdapter());
    }

    protected HoldingAreaService getHoldingAreaService(String adapterName) throws Exception {
        ImportAdapterFactory<T> adaptFac =
                getImportAdapterFactory(adapterName)
                        .orElse(null);
        if (adaptFac == null) {
            String message;
            if (ALPHANUMERIC.matcher(adapterName).matches()) {
                message = "Cannot predict settings with unknown import adapter: " + adapterName;
            } else {
                message = "Cannot predict settings with unknown import adapter";
            }
            throw new IOException(message);
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
        _holdingAreaService=service;
        return service;
    }

    private ImportTaskMetaData<T> predictSettings(ImportTaskMetaData<T> task, Map<String, String> inputParameters) throws Exception {
        log.trace("in predictSettings, task for file: {}  with payload: {}", task.getFilename(), task.payloadID);
        ImportAdapterFactory<T> adaptFac = fetchAdapterFactory(task);
        adaptFac.setInputParameters(task.inputSettings);
        log.trace("got back adaptFac with name: {}", adaptFac.getAdapterName());
        Optional<InputStream> iStream = payloadService.getPayloadAsInputStream(task.payloadID);
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode parameters = mapper.convertValue(inputParameters, ObjectNode.class);
        ImportAdapterStatistics predictedSettings = adaptFac.predictSettings(iStream.get(), parameters);

        ImportTaskMetaData<T> newMeta = task.copy();
        if (predictedSettings != null) {
            newMeta.adapterSettings = predictedSettings.getAdapterSettings();
            newMeta.adapterSchema = predictedSettings.getAdapterSchema();
        } else {
            ObjectNode messageNode = JsonNodeFactory.instance.objectNode();
            messageNode.put("Message", "Error predicting settings");
        }
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
    protected final Map<UUID, ImportTaskMetaData> importTaskCache = new ConcurrentHashMap<>();

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

    public List<String> getImportAdapterNames() {
        return gsrsImportAdapterFactoryFactory.getAvailableAdapterNames(this.getEntityService().getContext());
    }

    public List<ClientFriendlyImportAdapterConfig> getConfiguredImportAdapters() {
        return gsrsImportAdapterFactoryFactory.getConfiguredAdapters(this.getEntityService().getContext(), this.getEntityService().getEntityClass());
    }

    public Optional<ImportAdapterFactory<T>> getImportAdapterFactory(String name) {
        log.trace(String.format("In getImportAdapterFactory, looking for adapter with name %s among %d", name, getImportAdapters().size()));
        if (getImportAdapters() != null) {
            getImportAdapters().forEach(a -> log.trace("adapter with name: {}, key: {}", a.getAdapterName(), a.getAdapterKey()));
        }
        Optional<ImportAdapterFactory<T>> adapterFactory = getImportAdapters().stream().filter(n -> name.equals(n.getAdapterName())).findFirst();
        if (!adapterFactory.isPresent()) {
            log.trace("searching for adapter by name failed; using key");
            adapterFactory = getImportAdapters().stream().filter(n -> name.equals(n.getAdapterKey())).findFirst();
        }
        return adapterFactory;
    }

    //todo: cleaner implementation:
    static protected HoldingAreaService _holdingAreaService;

    static public HoldingAreaService getHoldingAreaService(){
        return _holdingAreaService;
    }

    private HoldingAreaService getDefaultHoldingAreaService() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        log.trace("starting in getDefaultHoldingAreaService");
        Class holdingAreaServiceClass = gsrsImportAdapterFactoryFactory.getDefaultHoldingAreaService(getEntityService().getContext());
        if( holdingAreaServiceClass== null){
            log.error("Error retrieving !");
            return null;
        }
        log.trace("got class {}", holdingAreaServiceClass.getName());
        Constructor constructor = holdingAreaServiceClass.getConstructor(String.class);
        Object o = constructor.newInstance(this.getEntityService().getContext());
        HoldingAreaService service = AutowireHelper.getInstance().autowireAndProxy((HoldingAreaService) o);
        log.trace("instantiated service");

        Class holdingAreaEntityServiceClass = gsrsImportAdapterFactoryFactory.getDefaultHoldingAreaEntityService(getEntityService().getContext());
        log.trace("going entity service class: {}", holdingAreaEntityServiceClass.getName());
        Constructor constructorEntityService = holdingAreaEntityServiceClass.getConstructor();
        Object o2= constructorEntityService.newInstance();
        log.trace("instantiated entity service");
        HoldingAreaEntityService entityService = AutowireHelper.getInstance().autowireAndProxy((HoldingAreaEntityService)o2);
        service.registerEntityService(entityService);
        log.trace("called registerEntityService with {}", entityService.getClass().getName());
        log.trace("finished in getDefaultHoldingAreaService");
        _holdingAreaService= service;
        return service;
    }

    //STEP 0: list adapter classes
    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import/adapters"})
    public ResponseEntity<Object> getImportAdapters(@RequestParam Map<String, String> queryParameters) throws IOException {
        log.trace("in getImportAdapters");
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(getConfiguredImportAdapters(), queryParameters), HttpStatus.OK);
    }

    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import/adapters/{adapterkey}/@schema"})
    public ResponseEntity<Object> getSpecificImportAdapter(@PathVariable("adapterkey") String adapterKey, @RequestParam Map<String, String> queryParameters) throws IOException {
        log.trace("in getSpecificImportAdapter, adapterKey: {}", adapterKey);
        List<ClientFriendlyImportAdapterConfig> outputList = getConfiguredImportAdapters().stream()
                .filter(a -> a.getAdapterKey().equals(adapterKey))
                .collect(Collectors.toList());
        if (outputList != null && outputList.size() > 0) {
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(outputList,
                    queryParameters), HttpStatus.OK);
        }
        ObjectNode outputNode = JsonNodeFactory.instance.objectNode();
        outputNode.put("message", String.format("No adapter found with key %s", adapterKey));
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(outputNode,
                queryParameters), HttpStatus.BAD_REQUEST);
    }

    //STEP 1: UPLOAD
    @hasAdminRole
    @PostGsrsRestApiMapping("/import")
    public ResponseEntity<Object> handleImport(@RequestParam("file") MultipartFile file,
                                               @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("handleImport start");
        try {
            //This follows 3 steps:
            // 1. save the file as a payload
            // 2. save an ImportTaskMetaData that wraps the payload
            // 3. return the ImportTaskMetaData

            String adapterName = queryParameters.get("adapter");
            log.trace("handleImport, adapterName: {}; platformTransactionManager: {}", adapterName, platformTransactionManager);
            String fileEncoding = queryParameters.get("fileEncoding");
            log.trace("fileEncoding: " + fileEncoding);
            String entityType = queryParameters.get("entityType");//type of domain object to create, eventually
            Objects.requireNonNull(entityType, "Must supply entityType (class of object to create)");

            //pass the rest of the queryParameters to the task so they can be used by the adapter
            ObjectMapper mapper = new ObjectMapper();
            JsonNode queryParameterNode = mapper.valueToTree(queryParameters);

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
            itmd.setEntityType(entityType);
            itmd.setFileEncoding(fileEncoding);
            itmd.setInputSettings(queryParameterNode);
            if (itmd.getAdapter() != null && itmd.getAdapterSettings() == null) {
                itmd = predictSettings(itmd, queryParameters);
                //save after we assign the fields we'll need later on
            }
            log.trace("queryParameterNode: {}", queryParameterNode);
            if (itmd.getAdapterSettings() == null) {
                log.trace("AdapterSettings was null");
                itmd.setAdapterSettings(JsonNodeFactory.instance.objectNode());
            }
            ((ObjectNode) itmd.getAdapterSettings()).set("parameters", queryParameterNode);
            log.trace("set parameter node");
            itmd = saveImportTask(itmd).get();

            if (itmd != null && itmd.adapterSettings != null) {
                log.trace("itmd.adapterSettings: {}", itmd.adapterSettings.toPrettyString());
            } else {
                log.warn("itmd.adapterSettings null");
            }
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(itmd, queryParameters), HttpStatus.OK);
        } catch (Throwable t) {
            log.error("Error in handleImport", t);
            throw t;
        }
    }

    //STEP 2: Retrieve
    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import({id})", "/import/{id}"})
    public ResponseEntity<Object> getImport(@PathVariable("id") String id,
                                            @RequestParam Map<String, String> queryParameters) throws IOException {
        log.trace("starting getImport");
        Optional<ImportTaskMetaData> obj = getImportTask(UUID.fromString(id));
        if (obj.isPresent()) {
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(obj.get(), queryParameters), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }

    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import/data({id})/{version}", "/import/data/{id}/{version}"})
    public ResponseEntity<Object> getImportData(@PathVariable("id") String id,
                                            @PathVariable("version") int version,
                                            @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("starting getImportData");
        log.trace("id: {} version: {}", id, version);
        HoldingAreaService service = getDefaultHoldingAreaService();
        List<ImportData> importDataList= service.getImportData(id);
        log.trace("getDataForRecord returned {}", importDataList.size());
        Optional<ImportData> foundData;
        if(version>0) {
            foundData= importDataList.stream().filter(i->i.getVersion()== version).findFirst();
        } else{
            foundData= importDataList.stream().max(Comparator.comparing(ImportData::getVersion));
        }

        if (foundData.isPresent()) {
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(foundData.get(), queryParameters), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }

    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import/metadata({id})", "/import/metadata/{id}"})
    public ResponseEntity<Object> getImportMetadata(@PathVariable("id") String id,
                                                @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("starting getImportMetadata");
        log.trace("id: {}", id);
        HoldingAreaService service = getDefaultHoldingAreaService();
        ImportMetadata importMetadata= service.getImportMetaData(id);
        log.trace("getDataForRecord returned {}", importMetadata==null ? "null" : "one record");

        if (importMetadata !=null) {
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(importMetadata, queryParameters), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }

    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/importdata({id})/{segment}", "/importdata/{id}/{segment}"})
    public ResponseEntity<Object> getImportDataFull(@PathVariable("id") String id,
                                                    @PathVariable("segment") String segment,
                                                    @RequestParam Map<String, String> queryParameters,
                                                    HttpServletRequest request) throws Exception {
        log.trace("starting getImportDataFull");
        return handleDataRetrieval(id, segment, queryParameters);
    }

    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/importdata({id})", "/importdata/{id}"})
    public ResponseEntity<Object> getImportDataFullNoSegment(@PathVariable("id") String id,
                                                    @RequestParam Map<String, String> queryParameters,
                                                    HttpServletRequest request) throws Exception {
        log.trace("starting getImportDataFullNoSegment");
        return handleDataRetrieval(id, null, queryParameters);
    }

    private ResponseEntity<Object> handleDataRetrieval(String id, String segment, Map<String, String> queryParameters) throws Exception {
        log.trace("starting handleDataRetrieval");
        String versionRaw = queryParameters.get("version");
        //String adapterName = queryParameters.get("adapter");

        int version=0;

        HoldingAreaService service;
        try {
            service= getDefaultHoldingAreaService();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            log.error("error setting up holding area service", e);
            ObjectNode messageNode = JsonNodeFactory.instance.objectNode();
            messageNode.put("Message", "Unable to obtain holding area service");
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(messageNode, queryParameters), HttpStatus.NOT_FOUND);
        }

        if(versionRaw!=null && versionRaw.trim().length()>0){
            try {
                version = Integer.parseInt(versionRaw);
            }
            catch (NumberFormatException numberFormatException){
                log.warn("Unable to create integer from version input {}.  Using default", versionRaw);
            }
        }

        log.trace("id: {} version: {} segment: {}", id, version, segment);
        //Objects.requireNonNull(adapterName, "Must supply adapter name to interpret input data");
        ImportData requestedDataItem = service.getImportDataByInstanceIdOrRecordId(id, version);

        if (requestedDataItem!=null) {
            log.trace(" found data ");
            log.trace(requestedDataItem.getData());
            ObjectMapper mapper = new ObjectMapper();
            JsonNode realData = mapper.readTree(requestedDataItem.getData());
            log.trace("converted to JsonNode");
            if(segment!=null && segment.trim().length()>0) {
                Object nativeObject= service.deserializeObject(requestedDataItem.getEntityClassName(), requestedDataItem.getData());
                log.trace("nativeObject type {}; object:", nativeObject.getClass().getName());
                log.trace(EntityUtils.EntityWrapper.of(nativeObject).toFullJson());
                EntityUtils.EntityWrapper nativeObjectWrapped = EntityUtils.EntityWrapper.of(nativeObject);
                PojoPointer p = PojoPointer.fromURIPath(segment);
                Optional<EntityUtils.EntityWrapper> specific= nativeObjectWrapped.at(p);
                if(specific.isPresent()) {
                    log.trace("specific data tree found: ");
                    log.trace(EntityUtils.EntityWrapper.of(specific.get()).toFullJson());
                    return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(specific.get().getValue(), queryParameters), HttpStatus.OK);
                } else {
                    //deal with not found
                    log.trace("specific part of data tree not found!");
                    return gsrsControllerConfiguration.handleNotFound(queryParameters);
                }
            }
            log.trace("readData type {} {}", realData.getNodeType(),  realData.toPrettyString());
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(realData, queryParameters), HttpStatus.OK);
        }
        ObjectNode messageNode = JsonNodeFactory.instance.objectNode();
        messageNode.put("Message", "No data found for user input");
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(messageNode, queryParameters), HttpStatus.NOT_FOUND);
    }

    //STEP 2.5: Retrieve & predict if needed
    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import({id})/@predict", "/import/{id}/@predict"})
    public ResponseEntity<Object> getImportPredict(@PathVariable("id") String id,
                                                   @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("starting getImportPredict");
        Optional<ImportTaskMetaData> obj = getImportTask(UUID.fromString(id));
        if (obj.isPresent()) {
            String adapterName = queryParameters.get("adapter");
            ImportTaskMetaData itmd = obj.get().copy();
            if (adapterName != null) {
                itmd.setAdapter(adapterName);
            }
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(predictSettings(itmd, queryParameters), queryParameters), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }

    //STEP 3: Configure / Update the parsing data (ImportTaskMetaData)
    @hasAdminRole
    @PutGsrsRestApiMapping(value = {"/import"})
    public ResponseEntity<Object> updateImport(@RequestBody JsonNode updatedJson,
                                               @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("in updateImport");
        ObjectMapper om = new ObjectMapper();
        ImportTaskMetaData itmd = om.treeToValue(updatedJson, ImportTaskMetaData.class);

        if (itmd.getAdapter() != null && itmd.getAdapterSettings() == null) {
            itmd = predictSettings(itmd, queryParameters);
        }

        //TODO: validation
        //override any existing task version
        Optional<ImportTaskMetaData> taskHolder = saveImportTask(itmd);
        if (taskHolder.isPresent()) {
            itmd = saveImportTask(itmd).get();
        } else {
            log.error("error saving ImportTaskMetaData! ");
        }
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(itmd, queryParameters), HttpStatus.OK);
    }

    //STEP 3.5: Preview import
    //May required additional work
    @hasAdminRole
    @PutGsrsRestApiMapping(value = {"/import({id})/@preview", "/import/{id}/@preview"})
    public ResponseEntity<Object> executePreview(@PathVariable("id") String id,
                                                 @RequestBody(required = false) JsonNode updatedJson,
                                                 @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("executePreview.  id: " + id);
        Optional<ImportTaskMetaData> obj = getImportTask(UUID.fromString(id));
        Stream<T> objectStream;
        if (obj.isPresent()) {
            log.trace("retrieved ImportTaskMetaData");
            //TODO: make async and do other stuff:
            ImportTaskMetaData retrievedTask = obj.get();
            HoldingAreaService service;
            ImportTaskMetaData usableTask;
            if(updatedJson!= null && updatedJson.size()>0) {
                ObjectMapper om = new ObjectMapper();
                ImportTaskMetaData taskFromInput = om.treeToValue(updatedJson, ImportTaskMetaData.class);
                if (taskFromInput.getAdapter() != null && taskFromInput.getAdapterSettings() == null) {
                    taskFromInput = predictSettings(taskFromInput, queryParameters);
                }
                log.trace("generating preview data using latest data");
                objectStream = generateObjects(taskFromInput, queryParameters);
                service=getHoldingAreaService(taskFromInput);
                usableTask = taskFromInput;
            } else {
                log.trace("generating preview data using earlier data");
                objectStream = generateObjects(retrievedTask, queryParameters);
                service=getHoldingAreaService(retrievedTask);
                usableTask= retrievedTask;
            }

            //todo: increase limit -- 10 will not work for most imports!
            long limit = Long.parseLong(queryParameters.getOrDefault("limit", "10"));
            log.trace("limit: {}", limit);

            ArrayNode previewNode = JsonNodeFactory.instance.arrayNode();

            ObjectMapper mapper = new ObjectMapper();
            objectStream.forEach(object->{
                try {
                    ObjectNode singleRecord = JsonNodeFactory.instance.objectNode();
                    JsonNode dataAsNode= mapper.readTree(mapper.writeValueAsString(object));
                    singleRecord.set("data", dataAsNode);
                    MatchedRecordSummary matchSummary=service.findMatchesForJson(usableTask.entityType, mapper.writeValueAsString(object));
                    JsonNode matchesAsNode = mapper.readTree(mapper.writeValueAsString(matchSummary));
                    singleRecord.set("matches", matchesAsNode);
                    previewNode.add(singleRecord);
                } catch (JsonProcessingException e) {
                    log.error("Error serializing imported GSRS object", e);
                    throw new RuntimeException(e);
                }
            });

            AtomicBoolean objectProcessingOK = new AtomicBoolean(true);

            ObjectNode returnNode = JsonNodeFactory.instance.objectNode();
            returnNode.put("complete success", objectProcessingOK.get());
            returnNode.set(String.format("data preview (limit: %d", limit), previewNode);

            /*log.trace("queryParameters:");
            queryParameters.keySet().forEach(k->log.trace("key: {}; value: {}", k, queryParameters.get(k)));*/
            return new ResponseEntity<>(returnNode, HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }

    //May required additional work
    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import({id})/@validate", "/import/{id}/@validate"})
    public ResponseEntity<Object> executeValidate(@PathVariable("id") String id,
                                                  @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("executeValidate.  id: " + id);

        String adapterName = queryParameters.get("adapter");
        HoldingAreaService holdingAreaService = getHoldingAreaService(adapterName);
        Object response = holdingAreaService.validateInstance(id);
        if (response == null) {
            ObjectNode responseNode = JsonNodeFactory.instance.objectNode();
            String message = "an error occurred while performing validation. Check the instanceId. Check the server logs";
            if (ALPHANUMERIC.matcher(adapterName).matches()) {
                message = String.format("an error occurred while performing validation. Check the instanceId %s. Check the server logs", id);
            }

            responseNode.put("message", message);
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(responseNode, queryParameters), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(response, queryParameters), HttpStatus.OK);
    }


    //search for records that have the same values for key fields
    @hasAdminRole
    @PostGsrsRestApiMapping(value = {"/import/matches"})
    public ResponseEntity<Object> findMatches(@RequestBody JsonNode entityJson,
                                              @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("in findMatches");
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
        if (queryParameters.get("version") != null) {
            try {
                int versionValue = Integer.valueOf(queryParameters.get("version"));
                version = versionValue;
            } catch (NumberFormatException ex) {
                //we just fall back on the original 0 value
            }
        }
        service.deleteRecord(id, version);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

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
        HoldingAreaService service;
        try {
            service = getHoldingAreaService(adapterName);
        } catch (IOException ex) {
            Map<String, String> messages = new HashMap<>();
            messages.put("message", ex.getMessage());
            return new ResponseEntity<>(messages, HttpStatus.BAD_REQUEST);
        }
        log.trace("retrieved service");
        int version = Integer.parseInt(queryParameters.get("version"));
        ImportMetadata data = service.getImportMetaData(id, version);
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
        List<ImportData> data = service.getImportData(recordId);
        log.trace("retrieved data for record");
        return new ResponseEntity<>(data, HttpStatus.OK);
    }

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
        HoldingAreaService service;
        try {
            service = getHoldingAreaService(adapterName);
        } catch (IOException ex) {
            Map<String, String> messages = new HashMap<>();
            messages.put("message", ex.getMessage());
            return new ResponseEntity<>(messages, HttpStatus.BAD_REQUEST);
        }
        log.trace("retrieved service");
        String data = service.getInstanceData(instanceId);
        return new ResponseEntity<>(data, HttpStatus.OK);
    }

    private String saveHoldingAreaRecord(HoldingAreaService service, String json, ImportTaskMetaData importTaskMetaData) {
        ImportRecordParameters parameters = ImportRecordParameters.builder()
                .jsonData(json)
                .entityClassName(importTaskMetaData.getEntityType())
                .formatType(importTaskMetaData.mimeType)
                //.rawData(importTaskMetaData.)
                .source(importTaskMetaData.getFilename())
                .adapterName(importTaskMetaData.adapter)
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
        ObjectNode resultNode = JsonNodeFactory.instance.objectNode();
        resultNode.put("Results", result);

        //TODO: validation ?????
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(resultNode, queryParameters), HttpStatus.OK);
    }

    //STEP 4: Execute import -- persist the domain entity
    //May need more work
    @hasAdminRole
    @PostGsrsRestApiMapping(value = {"/import({id})/@execute", "/import/{id}/@execute"})
    public ResponseEntity<Object> executeImport(@PathVariable("id") String id,
                                                @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("starting executeImport");
        Optional<ImportTaskMetaData> obj = getImportTask(UUID.fromString(id));
        if (obj.isPresent()) {
            log.trace("retrieved ImportTaskMetaData");
            //TODO: make async and do other stuff:
            ImportTaskMetaData itmd = obj.get();

            //todo: increase limit -- 10 will not work for most imports!
            long limit = Long.parseLong(queryParameters.getOrDefault("limit", "10"));
            log.trace("limit: {}", limit);

            Stream<T> objectStream = generateObjects(itmd, queryParameters);

            HoldingAreaService service = getHoldingAreaService(itmd);
            ObjectMapper mapper = new ObjectMapper();
            AtomicBoolean objectProcessingOK = new AtomicBoolean(true);
            AtomicInteger recordCount = new AtomicInteger(0);
            List<Integer> errorRecords = new ArrayList<>();
            List<String> importDataRecordIds = new ArrayList<>();
            ArrayNode previewNode = JsonNodeFactory.instance.arrayNode();
            objectStream.forEach(object -> {
                recordCount.incrementAndGet();
                log.trace("going to call saveHoldingAreaRecord with data of type {}", object.getClass().getName());
                log.trace(object.toString());
                try {
                    importDataRecordIds.add(saveHoldingAreaRecord(service, mapper.writeValueAsString(object), itmd));
                    if (recordCount.get() < limit) {
                        if (object instanceof Supplier) {
                            log.trace("going to invoke supplier on object");
                            object = (T) ((Supplier) object).get();
                        }
                        //previewNode.add(mapper.writeValueAsString(object));
                        ObjectNode singleRecord = JsonNodeFactory.instance.objectNode();
                        JsonNode dataAsNode= mapper.readTree(mapper.writeValueAsString(object));
                        singleRecord.set("data", dataAsNode);
                        MatchedRecordSummary matchSummary=service.findMatchesForJson(itmd.entityType, mapper.writeValueAsString(object));
                        JsonNode matchesAsNode = mapper.readTree(mapper.writeValueAsString(matchSummary));
                        singleRecord.set("matches", matchesAsNode);
                        previewNode.add(singleRecord);
                    }
                } catch (JsonProcessingException e) {
                    objectProcessingOK.set(false);
                    errorRecords.add(recordCount.get());
                    log.error("Error processing staging area record", e);
                }
            });

            ObjectNode returnNode = JsonNodeFactory.instance.objectNode();
            returnNode.put("complete success", objectProcessingOK.get());
            ArrayNode recordIdListNode = JsonNodeFactory.instance.arrayNode();
            importDataRecordIds.forEach(recordIdListNode::add);
            returnNode.set("staging area record IDs", recordIdListNode);
            ArrayNode problemRecords = JsonNodeFactory.instance.arrayNode();
            errorRecords.forEach(problemRecords::add);
            returnNode.set("Records with processing errors", problemRecords);
            returnNode.set(String.format("data preview (limit: %d", limit), previewNode);

            /*log.trace("queryParameters:");
            queryParameters.keySet().forEach(k->log.trace("key: {}; value: {}", k, queryParameters.get(k)));*/
            return new ResponseEntity<>(returnNode, HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }

    @hasAdminRole
    @PutGsrsRestApiMapping(value = {"/import({id})/{version}/@act", "/import/{id}/{version}/@act"})
    public ResponseEntity<Object> executeAct(@PathVariable("id") String holdingRecordId,
                                             @PathVariable("version") int version,
                                             @RequestBody String processingJson,
                                             @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("starting executeAct");
        String adapterName = queryParameters.get("adapter");
        String matchedEntityId = queryParameters.get("matchedEntityId");
        String persist = queryParameters.get("persistChangedObject");
        HoldingAreaService holdingAreaService = getHoldingAreaService(adapterName);
        String objectJson="";
        String objectClass="";
        ImportData importData = holdingAreaService.getImportDataByInstanceIdOrRecordId( holdingRecordId, version);
        int realVersion = 0;
        ObjectNode messageNode = JsonNodeFactory.instance.objectNode();

        if( importData!= null) {
            log.trace("Data for id {} retrieved", holdingRecordId);
            objectJson = importData.getData();
            objectClass=importData.getEntityClassName();
            ImportMetadata metadata = holdingAreaService.getImportMetaData(importData.getInstanceId().toString(), importData.getVersion());
            if(metadata.getImportStatus()== ImportMetadata.RecordImportStatus.imported){
                messageNode.put("message", String.format("Error: staging area record with ID %s has already been imported",
                        holdingRecordId));
                return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(messageNode, queryParameters), HttpStatus.BAD_REQUEST);
            }
        }
        log.trace("objectJson: {}", objectJson);
        if( objectJson==null || objectJson.length()==0){
            messageNode.put("message", String.format("Error retrieving staging area object of type %s with ID %s",
                    objectClass, holdingRecordId));
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(messageNode, queryParameters), HttpStatus.BAD_REQUEST);
        }

        //make sure class type is the same as the holdingAreaService's entity name!
        log.trace("going to retrieve existing object of type {} with ID {}", objectClass, matchedEntityId);
        T baseObject = holdingAreaService.retrieveEntity(objectClass, matchedEntityId);
        if (baseObject == null) {
            messageNode.put("message", String.format("Error retrieving base object of type %s with ID %s",
                    objectClass, matchedEntityId));
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(messageNode, queryParameters), HttpStatus.BAD_REQUEST);
        }

        T currentObject = holdingAreaService.deserializeObject(objectClass, objectJson);
        if (currentObject == null) {
            messageNode.put("message", String.format("Error retrieving imported object of type %s with ID %s",
                    objectClass, matchedEntityId));
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(messageNode, queryParameters), HttpStatus.BAD_REQUEST);
        }

        StringBuilder whatHappened = new StringBuilder();
        whatHappened.append("Processing record: ");
        ObjectMapper mapper = new ObjectMapper();
        ProcessingActionConfigSet configSet = mapper.readValue(processingJson, ProcessingActionConfigSet.class);
        ImportMetadata.RecordImportStatus recordImportStatus;
        for (ProcessingActionConfig configItem : configSet.getProcessingActions()) {
            ProcessingAction action = (ProcessingAction) configItem.getProcessingActionClass().getConstructor().newInstance();
            log.trace("going to call action {}", action.getClass().getName());
            currentObject = (T) action.process(currentObject, baseObject, configItem.getParameters(), whatHappened::append);
            if(action.getClass().getName().toUpperCase().contains("MERGE")) {
                recordImportStatus = ImportMetadata.RecordImportStatus.merged;
            }
        }
        log.trace(whatHappened.toString());
        if(persist!=null && persist.equalsIgnoreCase("TRUE")){
            GsrsEntityService.ProcessResult<T>result =holdingAreaService.persistPermanentEntity(objectClass, currentObject);
            if(!result.isSaved()) {
                log.error("Error! Saved object is null");
                messageNode.put("Message", "Object failed to save");
                messageNode.put("Error", result.getThrowable().getMessage());
                return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(messageNode, queryParameters), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            currentObject =result.getEntity();
            recordImportStatus = ImportMetadata.RecordImportStatus.imported;
            holdingAreaService.updateRecordImportStatus(UUID.fromString(holdingRecordId), recordImportStatus);
            log.trace("saved updated entity");
        }else {
            log.trace("skipped saving");
        }
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(currentObject, queryParameters), HttpStatus.OK);
    }


    @GetGsrsRestApiMapping(value = "/importdatasearch", apiVersions = 1)
    public ResponseEntity<Object> searchImportData(@RequestParam("q") Optional<String> query,
                                           @RequestParam("top") Optional<Integer> top,
                                           @RequestParam("skip") Optional<Integer> skip,
                                           @RequestParam("fdim") Optional<Integer> fdim,
                                           HttpServletRequest request,
                                           @RequestParam Map<String, String> queryParameters){
        log.trace("searchImportData. Query: {}; kind: {}", query, getEntityService().getEntityClass().getName());
        SearchRequest.Builder builder = new SearchRequest.Builder()
                .query(query.orElse(null))
                .kind(getEntityService().getEntityClass());

        top.ifPresent( t-> builder.top(t));
        skip.ifPresent( t-> builder.skip(t));
        fdim.ifPresent( t-> builder.fdim(t));

        SearchRequest searchRequest = builder.withParameters(request.getParameterMap())
                .build();

        this.instrumentSearchRequest(searchRequest);

        SearchResult result = null;
        try {
            result = getlegacyGsrsSearchService().search(searchRequest.getQuery(), searchRequest.getOptions() );
        } catch (Exception e) {
            return getGsrsControllerConfiguration().handleError(e, queryParameters);
        }

        SearchResult fresult=result;

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setReadOnly(true);
        List results = (List) transactionTemplate.execute(stauts -> {
            //the top and skip settings  look wrong, because we're not skipping
            //anything, but it's actually right,
            //because the original request did the skipping.
            //This mechanism should probably be worked out
            //better, as it's not consistent.

            //Note that the SearchResult uses a LazyList,
            //but this is copying to a real list, this will
            //trigger direct fetches from the lazylist.
            //With proper caching there should be no further
            //triggered fetching after this.

            String viewType=queryParameters.get("view");
            if("key".equals(viewType)){
                List<ix.core.util.EntityUtils.Key> klist=new ArrayList<>(Math.min(fresult.getCount(),1000));
                fresult.copyKeysTo(klist, 0, top.orElse(10), true);
                return klist;
            }else{
                List tlist = new ArrayList<>(top.orElse(10));
                fresult.copyTo(tlist, 0, top.orElse(10), true);
                return tlist;
            }
        });


        //even if list is empty we want to return an empty list not a 404
        ResponseEntity<Object> ret= new ResponseEntity<>(createSearchResponse(results, result, request), HttpStatus.OK);
        return ret;
    }

}