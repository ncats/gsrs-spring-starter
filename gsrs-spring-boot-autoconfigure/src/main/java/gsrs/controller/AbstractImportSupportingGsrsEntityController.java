package gsrs.controller;

import gsrs.dataExchange.model.ImportFieldHandling;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.Collection;

public abstract class AbstractImportSupportingGsrsEntityController<C extends AbstractImportSupportingGsrsEntityController, T, I >
        extends AbstractGsrsEntityController<C, T, I> {

    @Autowired
    private PayloadService payloadService;
    @Autowired
    private PayloadRepository payloadRepository;
    @Autowired
    private PlatformTransactionManager platformTransactionManager;
        
    @Autowired
    private GsrsControllerConfiguration controllerConfiguration;

  
        
    @Data
    public class ImportTaskMetaData{
            
          private UUID id;
          
          //TODO: work on this
          private String adapter;
          private JsonNode adapterSettings;
          private JsonNode adapterSchema;
            
          //imported from payload
          private UUID payloadID;
          private String filename;
          private Long size;
          private String mimeType;  
          
          public Optional<Payload> fetchPayload(){
                return payloadRepository.findById(payloadId);
          }
          
          public Stream<T> execute(){
                 return fetchAdapterFactory()
                         .createAdapter(adapterSettings)
                         .parse(payloadService.getPayloadAsInputStream(this.payloadID));
          }
            
          public ImportTaskMetaData copy(){
                  ImportTaskMetaData task = new ImportTaskMetaData();
                  task.id = this.id;
                  task.payloadID=this.payloadID;
                  task.size=this.size;
                  task.mimeType=this.mimeType;
                  task.filename=this.filename;
                  
                  task.adapter=this.adapter;
                  task.adapterSettings=this.adapterSettings;
                  task.adapterSchema=this.adapterSchema;
                  
                  return task;
          }
            
          public ImportAdapterFactory<T> fetchAdapterFactory() throws Exception{
                 if(this.adapter==null){
                         throw new IOException("Cannot predict settings with null import adapter");
                 }
                 ImportAdapterFactory<T> adaptFac= getImportAdapterFactory(adapter).orElse(null);
                 if(this.adaptFac==null){
                         throw new IOException("Cannot predict settings with unknown import adapter:\"" + adapter + "\"");
                 }
                 return adaptFac;
          }
            
          public ImportTaskMetaData predictSettings() throws Exception{
                 ImportAdapterFactory<T> adaptFac= fetchAdapterFactory();
                 InputStream iStream =  payloadService.getPayloadAsInputStream(this.payloadID);
                 ImportAdapterStatistics predictedSettings = adaptFac.predictSettings(iStream);
                 
                 ImportTaskMetaData newMeta = ImportTaskMetaData.copy();
                 newMeta.adapterSettings=predictedSettings.adapterSettings;
                 newMeta.adapterSchema=predictedSettings.adapterSchema;

                 return newMeta;
          }
            
            
            
          public static ImportTaskMetaData from(Payload p){
                  ImportTaskMetaData task = new ImportTaskMetaData();
                  task.id = UUID.randomUUID();
                  task.payloadID=p.id;
                  task.size=p.size;
                  task.mimeType=p.mimeType;
                  task.filename=p.filename;
                  return task;
          }
          //TODO: add _self link
    }
    
    
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //MECHANISM TO SAVE IMPORT META DATA
    //TODO: move to better data store, perhaps more like export/download service
    private Map<UUID,ImportTaskMetaData> importTaskCache = new ConcurrentHashMap<>();
        
    private Optional<ImportTaskMetaData> getImportTask(UUID id){
            return Optional.ofNullable(importTaskCache.get(id));
    }
    
    private Optional<ImportTaskMetaData> saveImportTask(ImportTaskMetaData importTask){
            if(importTask.id==null){
             importTask.id=UUID.randomUUID();
            }
            importTaskCache.put(importTask.id, importTask);
            return importTask;
    }
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        
        
    public static interface ImportAdapter<T>{
            public Stream<T> parse(InputStream is);
    }
        
    public static interface ImportAdapterFactory<T>{
            public String getAdapterName();
            public List<String> getSupportedFileExtensions();           
            
            public ImportAdapter<T> createAdapter(JsonNode adapterSettings);
            public ImportAdapterStatistics predictSettings(InputStream is);
    }
        
    @Data
    public static class ImportAdapterStatistics{
        private JsonNode adapterSettings;
        private JsonNode adapterSchema;
    }
        
    //TODO: Override in specific repos AND eventually use config parsing mechanism
    public List<ImportAdapterFactory<T>> getImportAdapters(){
        return new ArrayList<>();         
    }
        
    public Optional<ImportAdapterFactory<T>> getImportAdapterFactory(String name){
        return getImportAdapters().stream().filter(n->name.equals(n.getAdapterName())).findFirst();
    }
            
   
        
    //STEP 0: list adapter classes
    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import/adapters"})
    public ResponseEntity<Object> getImport(@RequestParam Map<String, String> queryParameters) throws IOException {
        return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(getImportAdapters(),queryParameters), HttpStatus.OK);
    }
        
    //STEP 1: UPLOAD
    @hasAdminRole
    @PostGsrsRestApiMapping("/import")
    public ResponseEntity<Object> handleImport(@RequestParam("file") MultipartFile file,
                               @RequestParam Map<String, String> queryParameters) throws IOException {
        try {
            //This follows 3 steps:
            // 1. save the file as a payload
            // 2. save an ImportTaskMetaData that wraps the payload
            // 3. return the ImportTaskMetaData
                
            String adapterName = queryParameters.get("adapter");
                    
                    
                
            TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
            transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                
            UUID payloadId = transactionTemplate.execute(status -> {
                try {
                    return payloadService.createPayload(file.getOriginalFilename(), 
                                                        PayloadController.predictMimeTypeFromFile(file),
                                                        file.getBytes(), 
                                                        PayloadService.PayloadPersistType.TEMP).id;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });

            Payload payload = payloadRepository.findById(payloadId).get();
            ImportTaskMetaData itmd = ImportTaskMetaData.from(payload);
            if(adapterName!=null){
                    itmd.setAdapter(adapterName);
            }
            itmd = saveImportTask(itmd);
            if(itmd.getAdapter()!=null && itmd.getAdapterSettings() == null){
                   itmd = itmd.predictSettings();
            }
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(itmd,queryParameters), HttpStatus.OK);
        }catch(Throwable t){
            t.printStackTrace();
            throw t;
        }
    }
        
    //STEP 2: Retrieve
    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import({id})", "/import/{id}"})
    public ResponseEntity<Object> getImport(@PathVariable("id") String id,
                                   @RequestParam Map<String, String> queryParameters) throws IOException {
        Optional<ImportTaskMetaData> obj = getImportTask(UUID.fromString(id)));
        if(obj.isPresent()){
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(obj.get(), queryParameters), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }
        
    //STEP 2.5: Retrieve & predict if needed
    @hasAdminRole
    @GetGsrsRestApiMapping(value = {"/import({id})/@predict", "/import/{id}/@predict"})
    public ResponseEntity<Object> getImport(@PathVariable("id") String id,
                                   @RequestParam Map<String, String> queryParameters) throws IOException {
        Optional<ImportTaskMetaData> obj = getImportTask(UUID.fromString(id)));
        if(obj.isPresent()){
            String adapterName = queryParameters.get("adapter");
            ImportTaskMetaData itmd = obj.get().copy();
            if(adapterName!=null){
                    itmd.setAdapter(adapterName);
            }
            return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(itmd.predictSettings(), queryParameters), HttpStatus.OK);
        }
        return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }
        
    //STEP 3: Configure / Update
    @hasAdminRole
    @PutGsrsRestApiMapping(value = {"/import"})
    public ResponseEntity<Object> updateImport(@RequestBody JsonNode updatedJson,
                                               @RequestParam Map<String, String> queryParameters) throws Exception {
           ObjectMapper om = new ObjectMapper();
           ImportTaskMetaData itmd = om.treeToValue(updatedJson,ImportTaskMetaData.class);
            
           if(itmd.getAdapter()!=null && itmd.getAdapterSettings() == null){
                   itmd = itmd.predictSettings();
           }
            
           //TODO: validation
           //override any existing task version
           itmd=saveImportTask(itmd);
           return ResponseEntity<>(GsrsControllerUtil.enhanceWithView(itmd, queryParameters), HttpStatus.OK);
    }
       
        
        

    //STEP 4: Execute import
        //!!!!NEEDS MORE WORK
    @hasAdminRole
    @PostGsrsRestApiMapping(value = {"/import({id})/@execute", "/import/{id}/@execute"})
    public ResponseEntity<Object> executeImport(@PathVariable("id") String id,
                                   @RequestParam Map<String, String> queryParameters) throws IOException {
           Optional<ImportTaskMetaData> obj = getImportTask(UUID.fromString(id)));
           if(obj.isPresent()){
                //TODO: make async and do other stuff:
                ImportTaskMetaData itmd = obj.get();
                
                itmd.execute()
                    .forEach(t->{
                       //TODO do something with this, likely put into some other area as a large JSON dump
                       //which will have further processing
                       System.out.println(t);
                    });
                 
                return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(itmd, queryParameters), HttpStatus.OK);
           }
           return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }
        
        
       
}
