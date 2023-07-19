package gsrs.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.controller.hateoas.IxContext;
import gsrs.imports.*;
import gsrs.payload.PayloadController;
import gsrs.repository.PayloadRepository;
import gsrs.security.GsrsSecurityUtils;
import gsrs.security.hasAdminRole;
import gsrs.service.PayloadService;
import gsrs.springUtils.AutowireHelper;
import gsrs.springUtils.StaticContextAccessor;
import gsrs.stagingarea.model.ImportData;
import gsrs.stagingarea.model.ImportMetadata;
import gsrs.stagingarea.model.MatchedRecordSummary;
import gsrs.stagingarea.model.SelectableObject;
import gsrs.stagingarea.service.StagingAreaEntityService;
import gsrs.stagingarea.service.StagingAreaService;
import ix.core.models.Payload;
import ix.core.models.Text;
import ix.core.search.SearchOptions;
import ix.core.search.SearchRequest;
import ix.core.search.SearchResult;
import ix.core.search.text.FacetMeta;
import ix.core.search.text.TextIndexer;
import ix.core.util.EntityUtils;
import ix.core.util.pojopointer.PojoPointer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
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
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.concurrent.locks.Lock;

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
    private ApplicationEventPublisher eventPublisher;

    private final CachedSupplier<List<ImportAdapterFactory<T>>> importAdapterFactories
            = CachedSupplier.of(() -> gsrsImportAdapterFactoryFactory.newFactory(getEntityService().getContext(),
            this.getEntityService().getEntityClass()));

    private final static Pattern ALPHANUMERIC = Pattern.compile("^[a-zA-Z0-9-]*$");

    private Lock lock = new ReentrantLock();

    private ConcurrentHashMap<String, StagingAreaService> stagingAreaServiceMap = new ConcurrentHashMap<>();

    @Data
    public static class ImportTaskMetaData<T> {

        @JsonIgnore
        private UUID internalUuid;

        public String getId() {
            return internalUuid!=null ? internalUuid.toString() : "";
        }

        public void setId(String newId) {
            if(newId!=null && newId.length()>0) {
                try {
                    this.internalUuid = UUID.fromString(newId);
                } catch (IllegalArgumentException e){
                    log.error("Error creating UUID from input {}", newId);
                }
            }
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

        private String stagingAreaRecordId;
        private String entityType;

        private JsonNode inputSettings;

        private String owner;

        private Long textId;

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

        public ImportTaskMetaData(String payloadID, String filename, long size, String mimeType, String id, String fileEncoding,
                                  String owner) {
            //todo: add checks for valid input
            this.payloadID = UUID.fromString(payloadID);
            this.filename = filename;
            this.size = size;
            this.mimeType = mimeType;
            this.internalUuid = UUID.fromString(id);
            this.fileEncoding = fileEncoding;
            this.owner = owner;
        }

        public ImportTaskMetaData<T> copy() {
            ImportTaskMetaData<T> task = new ImportTaskMetaData<>();
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
            task.owner = this.owner;

            return task;
        }

        @Override
        public String toString() {
            StringBuilder returnBuilder = new StringBuilder();
            returnBuilder.append("\"internalUuid\": \"");
            returnBuilder.append(internalUuid);
            returnBuilder.append("\",\n");
            returnBuilder.append("\"adapter\": \"");
            returnBuilder.append(this.adapter);
            returnBuilder.append("\",\n");
            returnBuilder.append("\"adapterSettings\": ");
            returnBuilder.append(this.adapterSettings == null ? "null" : this.adapterSettings.toPrettyString());
            returnBuilder.append(",\n");
            returnBuilder.append("\"adapterSchema\": ");
            returnBuilder.append(this.adapterSchema == null ? "null" : this.adapterSchema.toPrettyString());
            returnBuilder.append(",\n");
            returnBuilder.append("\"entityType\": \"");
            returnBuilder.append(this.entityType);
            returnBuilder.append("\",\n");
            returnBuilder.append("\"payloadID\": \"");
            returnBuilder.append(this.payloadID);
            returnBuilder.append("\"\n");
            returnBuilder.append("\"owner\": \"");
            returnBuilder.append(this.owner);
            returnBuilder.append("\"");
            return returnBuilder.toString();
        }
        //TODO: add _self link


        public static ImportTaskMetaData fromText(Text text) throws JsonProcessingException {
            log.trace("starting in fromText");
            ObjectMapper mapper = new ObjectMapper();
            ImportTaskMetaData task = mapper.readValue(text.getValue(), ImportTaskMetaData.class);
            if (task == null) {
                log.error("Error creating ImportTaskMetaData from input {}", text.getValue());
                return null;
            }
            if (text.id != null) {
                task.setTextId(text.id);
            }
            return task;
        }

        public Text asText() {
            return new Text(getEntityKeyFromClass(entityType), EntityUtils.EntityWrapper.of(this).toInternalJson());
        }

        public static String getEntityKeyFromClass(String className) {
            return "import config " + className;
        }
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
        log.trace("in fetchAdapterFactory, staging area service: {}", adaptFac.getStagingAreaService().getName());
        adaptFac.setFileName(task.filename);
        return adaptFac;
    }


    private StagingAreaService getStagingAreaService(ImportTaskMetaData<T> task) throws Exception {
        return gsrsImportAdapterFactoryFactory.getStagingAreaService(this.getEntityService().getContext());
        /*Objects.requireNonNull(task.adapter, "Cannot predict settings with null import adapter");
        return getStagingAreaService(task.getAdapter());*/
    }

    protected StagingAreaService getStagingAreaService(String adapterName) throws Exception {
        log.trace("starting getStagingAreaService with adapterName {}", adapterName);
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
        stagingAreaServiceMap.computeIfAbsent(adaptFac.getStagingAreaService().getName(), (cls)->{
            Class<T> c = adaptFac.getStagingAreaService();
            log.trace("in getStagingAreaService, instantiating StagingAreaService: {}", c.getName());
            Constructor<T> constructor = null;
            try {
                constructor = c.getConstructor();
                Object o = constructor.newInstance();
                StagingAreaService service = AutowireHelper.getInstance().autowireAndProxy((StagingAreaService) o);
                log.trace("adaptFac.getEntityServiceClass(): {}", adaptFac.getEntityServiceClass());
                if (adaptFac.getEntityServiceClass() != null) {
                    Constructor entityServiceConstructor = adaptFac.getEntityServiceClass().getConstructor();
                    StagingAreaEntityService<T> entityService = (StagingAreaEntityService) entityServiceConstructor.newInstance();
                    entityService = AutowireHelper.getInstance().autowireAndProxy(entityService);
                    service.registerEntityService(entityService);
                    log.trace("called registerEntityService with {}", entityService.getClass().getName());
                    return service;
                } else {
                    log.warn("No entity service found.  using alternative strategy.");
                    _stagingAreaService = gsrsImportAdapterFactoryFactory.getStagingAreaService(getEntityService().getContext());
                    return _stagingAreaService;
                }

            } catch (Exception e) {
                log.error("Error instantiating staging area service of type {}", c.getName());
                throw new RuntimeException(e);
            }
        });
        Class<T> c = adaptFac.getStagingAreaService();
        log.trace("in getStagingAreaService, instantiating StagingAreaService: {}", c.getName());
        Constructor<T> constructor = c.getConstructor();
        Object o = constructor.newInstance();
        StagingAreaService service = AutowireHelper.getInstance().autowireAndProxy((StagingAreaService) o);
        log.trace("adaptFac.getEntityServiceClass(): {}", adaptFac.getEntityServiceClass());
        if (adaptFac.getEntityServiceClass() != null) {
            Constructor entityServiceConstructor = adaptFac.getEntityServiceClass().getConstructor();
            StagingAreaEntityService<T> entityService = (StagingAreaEntityService) entityServiceConstructor.newInstance();
            entityService = AutowireHelper.getInstance().autowireAndProxy(entityService);
            service.registerEntityService(entityService);
            log.trace("called registerEntityService with {}", entityService.getClass().getName());
        } else {
            log.warn("No entity service found.  using alternative strategy.");
            _stagingAreaService = gsrsImportAdapterFactoryFactory.getStagingAreaService(getEntityService().getContext());
            return _stagingAreaService;
        }
        _stagingAreaService = service;
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
        ImportAdapterStatistics predictedSettings = iStream.isPresent() ? adaptFac.predictSettings(iStream.get(), parameters):null;

        ImportTaskMetaData<T> newMeta = task.copy();
        if (predictedSettings != null) {
            newMeta.setAdapterSettings(predictedSettings.getAdapterSettings());
            newMeta.setAdapterSchema( predictedSettings.getAdapterSchema());
        } else {
            ObjectNode messageNode = JsonNodeFactory.instance.objectNode();
            messageNode.put("message", "Error predicting settings");
        }
        return newMeta;
    }

    public ImportTaskMetaData<T> from(Payload p) {
        ImportTaskMetaData<T> task = new ImportTaskMetaData<>();
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
    protected final Map<UUID, ImportTaskMetaData<T>> importTaskCache = new ConcurrentHashMap<>();

    private Optional<ImportTaskMetaData<T>> getImportTask(UUID id) {
        return Optional.ofNullable(importTaskCache.get(id));
    }

    private Optional<ImportTaskMetaData<T>> getImportTask(JsonNode node) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return Optional.of( mapper.treeToValue(node, ImportTaskMetaData.class));
        } catch (JsonProcessingException e) {
            log.error("Error converting JsonNode to ImportTaskMetaData");
            return Optional.empty();
        }
    }

    private Optional<ImportTaskMetaData<T>> saveImportTaskToCache(ImportTaskMetaData importTask) {
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
        log.trace("In getImportAdapterFactory, looking for adapter with name '{}' among {}", name, getImportAdapters().size());
        if (getImportAdapters() != null) {
            getImportAdapters().forEach(a -> log.trace("adapter with name: '{}', key: '{}'", a.getAdapterName(), a.getAdapterKey()));
        }
        Optional<ImportAdapterFactory<T>> adapterFactory = getImportAdapters().stream().filter(n -> name.equals(n.getAdapterName())).findFirst();
        if (!adapterFactory.isPresent()) {
            log.trace("searching for adapter by name failed; using key");
            adapterFactory = getImportAdapters().stream().filter(n -> name.equals(n.getAdapterKey())).findFirst();
            log.trace("success? {}", adapterFactory.isPresent());
        }
        return adapterFactory;
    }

    //todo: cleaner implementation:
    static protected StagingAreaService _stagingAreaService = null;

    protected StagingAreaService getDefaultStagingAreaService() throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return gsrsImportAdapterFactoryFactory.getStagingAreaService(getEntityService().getContext());
/*
        if(_stagingAreaService == null) {
            lock.lock();
            try {
                if(_stagingAreaService==null) {
                    _stagingAreaService = gsrsImportAdapterFactoryFactory.getStagingAreaService(getEntityService().getContext());
                }
            }finally {
                lock.unlock();
            }
        }
        return _stagingAreaService;
*/
    }

    //STEP 0: list adapter classes
    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import/adapters"}, produces = {"application/json"})
    public ResponseEntity<Object> getImportAdapters(@RequestParam Map<String, String> queryParameters) {
        log.trace("in getImportAdapters");
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(getConfiguredImportAdapters(), queryParameters), HttpStatus.OK);
    }

    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import/adapters/{adapterkey}/@schema"})
    public ResponseEntity<Object> getSpecificImportAdapter(@PathVariable("adapterkey") String adapterKey, @RequestParam Map<String, String> queryParameters) {
        log.trace("in getSpecificImportAdapter, adapterKey: {}", adapterKey);
        List<ClientFriendlyImportAdapterConfig> outputList = getConfiguredImportAdapters().stream()
                .filter(a -> a.getAdapterKey().equals(adapterKey))
                .collect(Collectors.toList());
        if ( outputList.size() > 0) {
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
            assert payloadId != null;
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
            itmd = saveImportTaskToCache(itmd).get();

            if (itmd != null && itmd.adapterSettings != null) {
                //log.trace("itmd.adapterSettings: {}", itmd.adapterSettings.toPrettyString());
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
                                            @RequestParam Map<String, String> queryParameters) {
        log.trace("starting getImport");
        Optional<ImportTaskMetaData<T>> obj = getImportTask(UUID.fromString(id));
        if (obj.isPresent()) {
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(obj.get(), queryParameters), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }

    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/stagingArea/metadata({id})", "/stagingArea/metadata/{id}"})
    public ResponseEntity<Object> getImportMetadata(@PathVariable("id") String id,
                                                    @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("starting getImportMetadata");
        log.trace("id: {}", id);
        StagingAreaService service = getDefaultStagingAreaService();
        ImportMetadata importMetadata = service.getImportMetaData(id);
        log.trace("getDataForRecord returned {}", importMetadata == null ? "null" : "one record");

        if (importMetadata != null) {
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(importMetadata, queryParameters), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }

    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/stagingArea({id})/{segment}", "/stagingArea/{id}/{segment}"})
    public ResponseEntity<Object> getImportDataFull(@PathVariable("id") String id,
                                                    @PathVariable("segment") String segment,
                                                    @RequestParam Map<String, String> queryParameters,
                                                    HttpServletRequest request) throws Exception {
        log.trace("starting getImportDataFull");
        return handleDataRetrieval(id, segment, queryParameters);
    }

    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/stagingArea({id})", "/stagingArea/{id}"})
    public ResponseEntity<Object> getImportDataFullNoSegment(@PathVariable("id") String id,
                                                             @RequestParam Map<String, String> queryParameters,
                                                             HttpServletRequest request) throws Exception {
        log.trace("starting getImportDataFullNoSegment");
        return handleDataRetrieval(id, null, queryParameters);
    }

    private ResponseEntity<Object> handleDataRetrieval(String id, String segment, Map<String, String> queryParameters) throws Exception {
        log.trace("starting handleDataRetrieval");
        String versionRaw = queryParameters.get("version");

        int version = 0;

        StagingAreaService service;
        try {
            service = getDefaultStagingAreaService();
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException |
                 IllegalAccessException e) {
            log.error("error setting up staging area service", e);
            ObjectNode messageNode = JsonNodeFactory.instance.objectNode();
            messageNode.put("message", "Unable to obtain staging area service");
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(messageNode, queryParameters), HttpStatus.NOT_FOUND);
        }

        if (versionRaw != null && versionRaw.trim().length() > 0) {
            try {
                version = Integer.parseInt(versionRaw);
            } catch (NumberFormatException numberFormatException) {
                log.warn("Unable to create integer from version input {}.  Using default", versionRaw);
            }
        }

        log.trace("id: {} version: {} segment: {}", id, version, segment);
        Objects.requireNonNull(service, "Must have a Staging Area Service work with data!");
        if(segment!= null && segment.equalsIgnoreCase("instances")){
            List<ImportData> data = service.getImportData(id);
            log.trace("retrieved data for record");
            return new ResponseEntity<>(data, HttpStatus.OK);
        }
        ImportData requestedDataItem = service.getImportDataByInstanceIdOrRecordId(id, version);
        ImportMetadata matchingMetadata;

        if (requestedDataItem != null) {
            log.trace(" found data ");
            //log.trace(requestedDataItem.getData());
            matchingMetadata = service.getImportMetaData(requestedDataItem.getRecordId().toString(), 0);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode realData = mapper.readTree(requestedDataItem.getData());
            log.trace("converted to JsonNode");
            if (segment != null && segment.trim().length() > 0) {
                Object nativeObject = service.deserializeObject(requestedDataItem.getEntityClassName(), requestedDataItem.getData());
                log.trace("nativeObject type {}; object:", nativeObject.getClass().getName());
                log.trace(EntityUtils.EntityWrapper.of(nativeObject).toFullJson());
                EntityUtils.EntityWrapper nativeObjectWrapped = EntityUtils.EntityWrapper.of(nativeObject);
                PojoPointer p = PojoPointer.fromURIPath(segment);
                Optional<EntityUtils.EntityWrapper> specific = nativeObjectWrapped.at(p);
                if (specific.isPresent()) {
                    log.trace("specific data tree found: ");
                    log.trace(EntityUtils.EntityWrapper.of(specific.get()).toFullJson());
                    return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(specific.get().getValue(), queryParameters), HttpStatus.OK);
                } else {
                    //deal with not found
                    log.trace("specific part of data tree not found!");
                    return gsrsControllerConfiguration.handleNotFound(queryParameters);
                }
            }
            if (realData instanceof ObjectNode) {
                ImportUtilities.enhanceWithMetadata((ObjectNode) realData, matchingMetadata, service);
            }
            //log.trace("readData type {} {}", realData.getNodeType(),  realData.toPrettyString());
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(realData, queryParameters), HttpStatus.OK);
        }
        ObjectNode messageNode = JsonNodeFactory.instance.objectNode();
        messageNode.put("message", "No data found for user input");
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(messageNode, queryParameters), HttpStatus.NOT_FOUND);
    }

    //STEP 2.5: Retrieve & predict if needed
    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import({id})/@predict", "/import/{id}/@predict"})
    public ResponseEntity<Object> getImportPredict(@PathVariable("id") String id,
                                                   @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("starting getImportPredict");
        Optional<ImportTaskMetaData<T>> obj = getImportTask(UUID.fromString(id));
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
        Optional<ImportTaskMetaData<T>> taskHolder = saveImportTaskToCache(itmd);
        if (taskHolder.isPresent()) {
            itmd = saveImportTaskToCache(itmd).get();
        } else {
            log.error("error saving ImportTaskMetaData! ");
        }
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(itmd, queryParameters), HttpStatus.OK);
    }

    //STEP 3.5: Preview import
    //May required additional work
    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import({id})/@preview", "/import/{id}/@preview"})
    public ResponseEntity<Object> executePreviewGet(@PathVariable("id") String id,
                                                 @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("executePreviewGet.  id: " + id);
        JsonNode previewResult = handlePreview(id, null, queryParameters);
        if( previewResult.isTextual()) {
            new ResponseEntity<>(previewResult, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(previewResult, HttpStatus.OK);
    }

    @hasAdminRole
    @PutGsrsRestApiMapping(value = {"/import({id})/@preview", "/import/{id}/@preview"})
    public ResponseEntity<Object> executePreviewPut(@PathVariable("id") String id,
                                                 @RequestBody(required = false) JsonNode updatedJson,
                                                 @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("executePreviewPut  id: " + id);
        JsonNode previewResult = handlePreview(id, updatedJson, queryParameters);
        if( previewResult.isTextual()) {
            new ResponseEntity<>(previewResult, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(previewResult, HttpStatus.OK);
    }

    //May required additional work
    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/stagingArea({id})/@validate", "/stagingArea/{id}/@validate"})
    public ResponseEntity<Object> executeValidate(@PathVariable("id") String id,
                                                  @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("executeValidate.  id: " + id);

        StagingAreaService stagingAreaService = getDefaultStagingAreaService();
        Object response = stagingAreaService.validateInstance(id);
        if (response == null) {
            ObjectNode responseNode = JsonNodeFactory.instance.objectNode();
            responseNode.put("message", "no data found for input");
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(responseNode, queryParameters), HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(response, queryParameters), HttpStatus.OK);
    }


    //search for records that have the same values for key fields
    @hasAdminRole
    @PostGsrsRestApiMapping(value = {"/stagingArea/matches"})
    public ResponseEntity<Object> findMatches(@RequestBody JsonNode entityJson,
                                              @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("in findMatches");
        String entityType = queryParameters.get("entityType");
        log.trace("entityType: " + entityType);
        log.trace("entityJson.toString(): " + entityJson.toString());
        StagingAreaService service = getDefaultStagingAreaService();
        log.trace("retrieved service");
        //findMatches
        MatchedRecordSummary summary = service.findMatchesForJson(entityType, entityJson.toString(), null);
        Object returned;
        if (queryParameters.containsKey("view") && "full".equalsIgnoreCase(queryParameters.get("view"))) {
            returned = summary;
        } else {
            returned = summary.getMatches();
        }
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(returned, queryParameters), HttpStatus.OK);
    }

    @hasAdminRole
    @DeleteGsrsRestApiMapping(value = {"/stagingArea({id})/@delete", "/stagingArea/{id}/@delete"})
    public ResponseEntity<Object> deleteRecord(@PathVariable("id") String id,
                                               @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("in deleteRecord");
        StagingAreaService stagingAreaService = getDefaultStagingAreaService();
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
        ImportUtilities<T> importUtilities = new ImportUtilities<>(getEntityService().getContext(), getEntityService().getEntityClass(),
                stagingAreaService);
        AutowireHelper.getInstance().autowire(importUtilities);
        importUtilities.removeImportMetadataFromIndex(id, version);
        stagingAreaService.deleteRecord(id, version);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @hasAdminRole
    @DeleteGsrsRestApiMapping(value = {"/stagingArea/@deletebulk", "/stagingArea/@bulkDelete"})
    public ResponseEntity<Object> deleteRecords(@RequestBody String idSet,
                                               @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("in deleteRecords");
        log.trace("retrieved service");
        int version = 0;//possible to retrieve from parameters
        log.trace("idSet: {}", idSet);
        String[] ids =idSet.split("[,\\r\\n]{1,2}");//idSet.split("\r{0,1}\n|\r");;
        boolean removeFromIndex = true;
        if( queryParameters.containsKey("skipIndex") && queryParameters.get("skipIndex").equalsIgnoreCase("true")){
            removeFromIndex=false;
        }
        StagingAreaService stagingAreaService = getDefaultStagingAreaService();
        ImportUtilities<T> importUtilities = new ImportUtilities<>(getEntityService().getContext(), getEntityService().getEntityClass(),
                stagingAreaService);
        AutowireHelper.getInstance().autowire(importUtilities);
        JsonNode returnNode = importUtilities.handleDeletion(ids, removeFromIndex);
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(returnNode, queryParameters), HttpStatus.OK);
    }

    @hasAdminRole
    @PutGsrsRestApiMapping(value = {"/stagingArea/{id}/@update", "/stagingArea({id})@update"})
    public ResponseEntity<Object> updateImportData(@PathVariable("id") String recordId,
                                                   @RequestBody String updatedJson,
                                                   @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("in updateImportData. ID: {} ", recordId);
        StagingAreaService service = getDefaultStagingAreaService();
        String cleanUpdatedJson = ImportUtilities.removeMetadataFromDomainObjectJson(updatedJson);
        log.trace("json: {}", cleanUpdatedJson);
        String result = service.updateRecord(recordId, cleanUpdatedJson);

        ObjectNode resultNode = JsonNodeFactory.instance.objectNode();
        resultNode.put("results", result);

        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(resultNode, queryParameters), HttpStatus.OK);
    }

    @hasAdminRole
    @PostGsrsRestApiMapping(value = {"/import({id})/@executeasync", "/import/{id}/@executeasync", "/import({id})/@execute",
            "/import/{id}/@execute"})
    public ResponseEntity<Object> executeImportAsync(@PathVariable("id") String id,
                                                     @RequestBody JsonNode updatedJson,
                                                     @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("starting executeImport");
        ImportTaskMetaData<T> updatedTask = null;
        StagingAreaService stagingAreaService = getDefaultStagingAreaService();
        ImportUtilities<T> importUtilities = new ImportUtilities<>(getEntityService().getContext(), getEntityService().getEntityClass(),
                stagingAreaService);
        AutowireHelper.getInstance().autowire(importUtilities);

        if(updatedJson !=null && updatedJson.size()>0) {
            log.trace("non-null updatedJson");
            Optional<ImportTaskMetaData<T>> updatedTaskHolder =getImportTask(updatedJson);
            if(updatedTaskHolder.isPresent()) {
                log.trace("converted input to something usable");
                JsonNode returnNode = importUtilities.handleObjectCreationAsync(updatedTaskHolder.get(), queryParameters);
                return new ResponseEntity<>(returnNode, HttpStatus.OK);
            }
        }
        Optional<ImportTaskMetaData<T>> importTask = getImportTask(UUID.fromString(id));
        if (importTask.isPresent()) {
            JsonNode returnNode = importUtilities.handleObjectCreationAsync(importTask.get(), queryParameters);
            return new ResponseEntity<>(returnNode, HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }

    @hasAdminRole
    @PutGsrsRestApiMapping(value = {"/import({id})/{version}/@act", "/import/{id}/{version}/@act"})
    public ResponseEntity<Object> executeAct(@PathVariable("id") String stagingRecordId,
                                             @PathVariable("version") int version,
                                             @RequestBody String processingJson,
                                             @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("starting executeAct");
        String matchedEntityId = queryParameters.get("matchedEntityId");
        String persist = queryParameters.get("persistChangedObject");
        StagingAreaService stagingAreaService = getDefaultStagingAreaService();
        ImportUtilities<T> importUtilities = new ImportUtilities<>(getEntityService().getContext(), getEntityService().getEntityClass(),
                stagingAreaService);
        importUtilities = AutowireHelper.getInstance().autowireAndProxy(importUtilities);
        ObjectNode resultNode = importUtilities.handleAction(stagingAreaService, matchedEntityId, stagingRecordId, version,
                persist, processingJson, getEntityService().getContext());
        log.trace("received return from importUtilities.handleAction");
        HttpStatus returnStatus = HttpStatus.resolve(resultNode.get("httpStatus").asInt());
        log.trace("resolved status: {}", returnStatus);
        if( resultNode.hasNonNull("object")) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            T object= mapper.readValue(resultNode.get("object").asText(), getEntityService().getEntityClass());
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(object, queryParameters), returnStatus);
        } else {
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(resultNode, queryParameters), returnStatus);
        }
    }

    @hasAdminRole
    @PutGsrsRestApiMapping(value = { "/stagingArea({id})/@act", "/stagingArea/{id}/@act"})
    public ResponseEntity<Object> executeAct2(@PathVariable("id") String stagingRecordId,
                                             @RequestBody String processingJson,
                                             @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("starting executeAct");
        String matchedEntityId = queryParameters.get("matchedEntityId");
        String persist = queryParameters.get("persistChangedObject");
        StagingAreaService stagingAreaService = getDefaultStagingAreaService();
        ImportUtilities<T> importUtilities = new ImportUtilities<>(getEntityService().getContext(), getEntityService().getEntityClass(),
                stagingAreaService);
        importUtilities = AutowireHelper.getInstance().autowireAndProxy(importUtilities);
        int version=0;//use last
        ObjectNode resultNode = importUtilities.handleAction(stagingAreaService, matchedEntityId, stagingRecordId, version,
                persist, processingJson, getEntityService().getContext());
        log.trace("received return from importUtilities.handleAction");
        HttpStatus returnStatus = HttpStatus.resolve(resultNode.get("httpStatus").asInt());
        log.trace("resolved status: {}", returnStatus);
        if( resultNode.hasNonNull("object")) {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            T object= mapper.readValue(resultNode.get("object").asText(), getEntityService().getEntityClass());
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(object, queryParameters), returnStatus);
        } else {
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(resultNode, queryParameters), returnStatus);
        }
    }

    @hasAdminRole
    @PutGsrsRestApiMapping(value = { "/stagingArea/@bulkactasync", "/stagingArea/@bulkAct"})
    public ResponseEntity<Object> executeActBulkAsync(
            @RequestBody String processingJson,
            @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("starting executeAct");
        String persist = queryParameters.get("persistChangedObject");
        StagingAreaService stagingAreaService = getDefaultStagingAreaService();
        ImportUtilities<T> importUtilities = new ImportUtilities<>(getEntityService().getContext(), getEntityService().getEntityClass(),
                stagingAreaService);
        importUtilities = AutowireHelper.getInstance().autowireAndProxy(importUtilities);
        int version=0;//use last
        JsonNode job = importUtilities.handleActionsAsync(stagingAreaService, version,
                persist, processingJson);
        return new ResponseEntity<>(job, HttpStatus.OK);
    }

    @hasAdminRole
    @GetGsrsRestApiMapping(value = { "/stagingArea/processingstatus({processingJobId})", "/stagingArea/processingstatus/{processingJobId}"})
    public ResponseEntity<Object> getProcessingStatus(
            @PathVariable("processingJobId") String processingJobId,
            @RequestParam Map<String, String> queryParameters) throws Exception {
        log.trace("starting getProcessingStatus");
        boolean includeJobInputData = false;
        if( queryParameters.containsKey("includeJobInputData")){
            try {
                includeJobInputData= Boolean.parseBoolean(queryParameters.get("includeJobInputData"));
            }
            catch (Exception ignore){}
        }
        log.trace("using processingJobId {}", processingJobId);
        if( processingJobId==null || processingJobId.length()==0){
            log.warn("no processingJobId supplied");
            ObjectNode messageNode = JsonNodeFactory.instance.objectNode();
            messageNode.put("message", "This method requires a valid value for processingJobId");
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(messageNode, queryParameters), HttpStatus.BAD_REQUEST);
        }
        ImportUtilities<T> importUtilities = new ImportUtilities<>(getEntityService().getContext(), getEntityService().getEntityClass(),
                getDefaultStagingAreaService());
        importUtilities = AutowireHelper.getInstance().autowireAndProxy(importUtilities);
        JsonNode jobNode = importUtilities.getJobAsNode(processingJobId, includeJobInputData);
        HttpStatus returnStatus = HttpStatus.OK;
        if(jobNode.hasNonNull("error")) {
            returnStatus= HttpStatus.BAD_REQUEST;
        }
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(jobNode, queryParameters), returnStatus);
    }

    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/stagingArea/search"}, apiVersions = 1)
    public ResponseEntity<Object> searchImportData(@RequestParam("q") Optional<String> query,
                                                   @RequestParam("top") Optional<Integer> top,
                                                   @RequestParam("skip") Optional<Integer> skip,
                                                   @RequestParam("fdim") Optional<Integer> fdim,
                                                   HttpServletRequest request,
                                                   @RequestParam Map<String, String> queryParameters) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        log.trace("searchImportData. Query: {}; kind: {}", query, getEntityService().getEntityClass().getName());
        SearchRequest.Builder builder = new SearchRequest.Builder()
                .query(query.orElse(null))
                .kind(ImportMetadata.class);

        top.ifPresent(builder::top);
        skip.ifPresent(builder::skip);
        fdim.ifPresent(builder::fdim);

        SearchRequest searchRequest = builder.withParameters(request.getParameterMap())
                .build();

        StagingAreaService service = getDefaultStagingAreaService();
        SearchResult result;
        try {
            result=service.findRecords(searchRequest, ImportTaskMetaData.class);
        } catch (Exception e) {
            return getGsrsControllerConfiguration().handleError(e, queryParameters);
        }

        SearchResult fresult = result;

        ObjectMapper mapper = new ObjectMapper();
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setReadOnly(true);
        List results = (List) transactionTemplate.execute(stats -> {
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

            String viewType = queryParameters.get("view");
            if ("key".equals(viewType)) {
                List<ix.core.util.EntityUtils.Key> klist = new ArrayList<>(Math.min(fresult.getCount(), 1000));
                fresult.copyKeysTo(klist, 0, top.orElse(10), true);
                return klist;
            } else if("selectable".equals(viewType)) {
                log.trace("selectable view");
                return (List<SelectableObject>) result.getMatches().stream()
                        .map(r -> {
                            ImportMetadata currentResult = (ImportMetadata) r;
                            log.trace("cast object to ImportMetadata");
                            SelectableObject selectableObject = new SelectableObject();
                            selectableObject.setId(currentResult.getRecordId() != null ? currentResult.getRecordId().toString() : "No ID");
                            selectableObject.setEntityClass(currentResult.getEntityClassName());
                            log.trace("set id and class");
                            log.trace("retrieved related data");
                            try {
                                JsonNode realData = mapper.readTree(service.getInstanceData(currentResult.getInstanceId().toString()));
                                log.trace("retrieved domain object JSON directly and turned it into a JsonNode");
                                selectableObject.setName(realData.get("_name").asText());
                                log.trace("gong name");
                            } catch (JsonProcessingException e) {
                                log.error("Error processing selectable search result", e);
                                throw new RuntimeException(e);
                            }
                            return selectableObject;
                        })
                        .collect(Collectors.toList());
            }
            else {
                List tlist = new ArrayList<>(top.orElse(10));
                fresult.copyTo(tlist, 0, top.orElse(10), true);
                return tlist;
            }
        });


        //some processing to return a List of domain objects with ImportMetadata appended
        List transformedResults = new ArrayList<>();

        results.forEach(r->{
            if( r instanceof ImportMetadata){
                ImportMetadata currentResult = (ImportMetadata)r;
                log.trace("looking at metadata object with record ID {}", currentResult.getRecordId());
                try {
                    JsonNode realData = mapper.readTree(service.getInstanceData(currentResult.getInstanceId().toString()));
                    log.trace("retrieved domain object JSON directly and turned it into a JsonNode");
                    ImportUtilities.enhanceWithMetadata((ObjectNode) realData, currentResult, service);
                    transformedResults.add(realData);
                } catch (JsonProcessingException e) {
                    log.error("Error processing search result", e);
                    throw new RuntimeException(e);
                }
            }
        });
        if( !transformedResults.isEmpty()) {
            log.trace("returning list of domain objects");
            return new ResponseEntity<>(createSearchResponse(transformedResults, result, request), HttpStatus.OK);
        }
        //even if list is empty we want to return an empty list not a 404
        return new ResponseEntity<>(createSearchResponse(results, result, request), HttpStatus.OK);
    }

    @hasAdminRole
    @GetGsrsRestApiMapping(value = "/stagingArea/search/@facets", apiVersions = 1)
    public FacetMeta searchImportFacetFieldDrilldownV1(@RequestParam("q") Optional<String> query,
                                                 @RequestParam("field") Optional<String> field,
                                                 @RequestParam("top") Optional<Integer> top,
                                                 @RequestParam("skip") Optional<Integer> skip,
                                                 HttpServletRequest request) throws ParseException, IOException {
        log.trace("fetching facets for ImportMetadata");
        SearchOptions so = new SearchOptions.Builder()
                .kind(ImportMetadata.class)
                .top(Integer.MAX_VALUE) // match Play GSRS
                .fdim(10)
                .fskip(0)
                .ffilter("")
                .withParameters(request.getParameterMap())
                .build();
        so = this.instrumentSearchOptions(so); //add user

        List<String> userLists = new ArrayList<>();
        String userName = "";
        if(GsrsSecurityUtils.getCurrentUsername().isPresent()) {
            userName = GsrsSecurityUtils.getCurrentUsername().get();
            userLists= userSavedListService.getUserSearchResultLists(userName);
        }

        TextIndexer.TermVectors tv= getlegacyGsrsSearchService().getTermVectorsFromQueryNew(query.orElse(null), so, field.orElse(null));
        return tv.getFacet(so.getFdim(), so.getFskip(), so.getFfilter(),
                StaticContextAccessor.getBean(IxContext.class).getEffectiveAdaptedURI(request).toString(),
                userName, userLists);

    }

    @hasAdminRole
    @PostGsrsRestApiMapping("/import/config")
    public ResponseEntity<Object> handleImportConfigSave(@RequestBody String importConfigJson,
                                                         @RequestParam Map<String, String> queryParameters) throws JsonProcessingException {
        log.trace("starting in handleImportConfigSave");
        ObjectMapper mapper = new ObjectMapper();
        ObjectMapper om = new ObjectMapper();
        ImportTaskMetaData itmd = om.readValue(importConfigJson, ImportTaskMetaData.class);

        if (itmd.getId() != null && ImportUtilities.doesImporterKeyExist(itmd.getId(), getEntityService().getEntityClass(), textRepository)) {
            ObjectNode resultNode = JsonNodeFactory.instance.objectNode();
            resultNode.put("Error in provided configuration", String.format("An Import configuration with id %s already exists in the database!",
                    itmd.getId()));
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(resultNode, queryParameters), HttpStatus.BAD_REQUEST);
        }
        itmd.setEntityType(getEntityService().getEntityClass().getName());
        itmd.setOwner(GsrsSecurityUtils.getCurrentUsername().isPresent() ? GsrsSecurityUtils.getCurrentUsername().get() : "unknown");

        Text textObject = itmd.asText();
        //deserialize importConfigJson to DefaultExporterFactoryConfig
        //generate keys rather take user input
        // keys must be systematic

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        Text savedText = transactionTemplate.execute(t -> textRepository.saveAndFlush(textObject));
        assert savedText!=null;
        log.trace("completed save of Text with ID {}", savedText.id);
        itmd.setTextId(savedText.id);
        savedText.setText(mapper.writeValueAsString(itmd));
        savedText.setIsDirty("configurationId");
        log.trace("called savedText.setIsDirty(");
        TransactionTemplate transactionTemplate2 = new TransactionTemplate(transactionManager);
        transactionTemplate2.executeWithoutResult(t -> textRepository.saveAndFlush(savedText));
        log.trace("completed 2nd save of Text");
        //todo: ID processing

        ObjectNode resultNode = JsonNodeFactory.instance.objectNode();
        resultNode.put("newly created configuration", savedText.id);
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(resultNode, queryParameters), HttpStatus.OK);
    }

    @hasAdminRole
    @GetGsrsRestApiMapping("/import/configs")
    public ResponseEntity<Object> handleGetImportConfigs(@RequestParam Map<String, String> queryParameters) throws JsonProcessingException {
        List<ImportTaskMetaData> importConfigs = ImportUtilities.getAllImportTasks(getEntityService().getEntityClass(), textRepository);
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(importConfigs, queryParameters), HttpStatus.OK);
    }

    @hasAdminRole
    @GetGsrsRestApiMapping( value = {"/import/config({id})", "/import/config/{id}"} )
    public ResponseEntity<Object> handleGetImportConfig(@RequestParam Map<String, String> queryParameters,
                                                        @PathVariable("id") Long textId) throws JsonProcessingException {
        ObjectNode messageNode = JsonNodeFactory.instance.objectNode();
        if( textId==null || textId<=0) {
            messageNode.put("message", "invalid input");
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(messageNode, queryParameters), HttpStatus.BAD_REQUEST);
        }
        Text text= textRepository.retrieveById(textId);
        if( text==null) {
            messageNode.put("message", "Configuration not found for input");
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(messageNode, queryParameters), HttpStatus.BAD_REQUEST);
        }
        ImportTaskMetaData importConfig = ImportTaskMetaData.fromText(text);
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(importConfig, queryParameters), HttpStatus.OK);
    }

    @hasAdminRole
    @GetGsrsRestApiMapping( value = {"/stagingArea/action({actionName})/@options", "/stagingArea/action/{actionName}/@options"} )
    public ResponseEntity<Object> handleGetProcessingActionOptions(@RequestParam Map<String, String> queryParameters,
                                                        @PathVariable("actionName") String actionName) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        log.trace("Starting handleGetProcessingActionOptions");
        ObjectNode messageNode = JsonNodeFactory.instance.objectNode();
        if( actionName==null || actionName.length()==0) {
            messageNode.put("message", "invalid input");
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(messageNode, queryParameters), HttpStatus.BAD_REQUEST);
        }
        ImportUtilities<T> importUtilities = new ImportUtilities<>(getEntityService().getContext(), getEntityService().getEntityClass(),
                getDefaultStagingAreaService());
        AutowireHelper.getInstance().autowire(importUtilities);
        List<String> options = importUtilities.getOptionsForAction(actionName);
        log.trace("received schema");
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(options, queryParameters), HttpStatus.OK);
    }

    @hasAdminRole
    @GetGsrsRestApiMapping( value = {"/stagingArea/action({actionName})/@schema", "/stagingArea/action/{actionName}/@schema"} )
    public ResponseEntity<Object> handleGetProcessingActionSchema(@RequestParam Map<String, String> queryParameters,
                                                                   @PathVariable("actionName") String actionName) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        log.trace("Starting handleGetProcessingActionOptions");
        ObjectNode messageNode = JsonNodeFactory.instance.objectNode();
        if( actionName==null || actionName.length()==0) {
            messageNode.put("message", "invalid input");
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(messageNode, queryParameters), HttpStatus.BAD_REQUEST);
        }
        ImportUtilities<T> importUtilities = new ImportUtilities<>(getEntityService().getContext(), getEntityService().getEntityClass(),
                getDefaultStagingAreaService());
        AutowireHelper.getInstance().autowire(importUtilities);
        JsonNode schema = importUtilities.getSchemaForAction(actionName);
        log.trace("received schema");
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(schema, queryParameters), HttpStatus.OK);
    }

    public JsonNode handlePreview(String id, JsonNode updatedJson, Map<String, String> queryParameters) throws Exception {
        log.trace("handlePreview; id: " + id);
        ImportUtilities<T> importUtilities = new ImportUtilities<>(getEntityService().getContext(), getEntityService().getEntityClass(),
                getDefaultStagingAreaService());
        AutowireHelper.getInstance().autowire(importUtilities);
        Optional<ImportTaskMetaData<T>> obj = getImportTask(UUID.fromString(id));
        Stream<T> objectStream;
        if (obj.isPresent()) {
            log.trace("retrieved ImportTaskMetaData");
            //TODO: make async and do other stuff:
            ImportTaskMetaData<T> retrievedTask = obj.get();
            StagingAreaService service;
            ImportTaskMetaData<T> usableTask;
            if (updatedJson != null && updatedJson.size() > 0) {
                ObjectMapper om = new ObjectMapper();
                ImportTaskMetaData<T> taskFromInput = om.treeToValue(updatedJson, ImportTaskMetaData.class);
                if (taskFromInput.getAdapter() != null && taskFromInput.getAdapterSettings() == null) {
                    taskFromInput = predictSettings(taskFromInput, queryParameters);
                }
                log.trace("generating preview data using latest data");
                objectStream = importUtilities.generateObjects(taskFromInput, queryParameters);
                service = getStagingAreaService(taskFromInput);
                usableTask = taskFromInput;
            } else {
                log.trace("generating preview data using earlier data");
                objectStream = importUtilities.generateObjects(retrievedTask, queryParameters);
                service = getStagingAreaService(retrievedTask);
                usableTask = retrievedTask;
            }

            long limit = 10;
            try{
                limit =Long.parseLong(queryParameters.getOrDefault("limit", "10"));
            } catch (NumberFormatException nfe) {
                log.warn("input for limit ({}) failed to parse.  Using default value", queryParameters.get("limit"));
            }
            log.trace("limit: {}", limit);

            ArrayNode previewNode = JsonNodeFactory.instance.arrayNode();

            ObjectMapper mapper = new ObjectMapper();
            objectStream.limit(limit).forEach(object -> {
                try {
                    ObjectNode singleRecord = JsonNodeFactory.instance.objectNode();
                    JsonNode dataAsNode = mapper.readTree(mapper.writeValueAsString(object));
                    singleRecord.set("data", dataAsNode);
                    MatchedRecordSummary matchSummary = service.findMatchesForJson(usableTask.entityType, mapper.writeValueAsString(object),
                            id);
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
            returnNode.put("completeSuccess", objectProcessingOK.get());
            returnNode.set("dataPreview", previewNode);
            returnNode.put("limit", limit);
            return returnNode;
        }
        return JsonNodeFactory.instance.textNode("No import data found using input");
    }

}