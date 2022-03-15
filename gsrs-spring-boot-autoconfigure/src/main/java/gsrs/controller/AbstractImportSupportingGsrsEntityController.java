package gsrs.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.payload.PayloadController;
import gsrs.repository.PayloadRepository;
import gsrs.security.hasAdminRole;
import gsrs.service.PayloadService;
import ix.core.models.Payload;
import lombok.Data;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import java.util.stream.Collectors;

public abstract class AbstractImportSupportingGsrsEntityController<C extends AbstractImportSupportingGsrsEntityController, T, I >
        extends AbstractLegacyTextSearchGsrsEntityController<C, T, I> {

    @Autowired
    private  PayloadService payloadService;
    @Autowired
    private  PayloadRepository payloadRepository;
    @Autowired
    private PlatformTransactionManager platformTransactionManager;
        
    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;


    @Data
    public static class ImportTaskMetaData<T>{

          private UUID myuuid;

          public String getId() {
            return myuuid.toString();
          }
          public void setId(String newId) {
              this.myuuid= UUID.fromString(newId);
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

          public ImportTaskMetaData() {
          }
          public ImportTaskMetaData(String payloadID, String filename, long size, String mimeType, String id ) {
              //todo: add checks for valid input
              this.payloadID=UUID.fromString(payloadID);
              this.filename=filename;
              this.size=size;
              this.mimeType=mimeType;
              this.myuuid= UUID.fromString(id);
          }
            
          public ImportTaskMetaData copy(){
                  ImportTaskMetaData task = new ImportTaskMetaData();
                  task.myuuid = this.myuuid;
                  task.payloadID=this.payloadID;
                  task.size=this.size;
                  task.mimeType=this.mimeType;
                  task.filename=this.filename;
                  
                  task.adapter=this.adapter;
                  task.adapterSettings=this.adapterSettings;
                  task.adapterSchema=this.adapterSchema;
                  
                  return task;
          }
            
          
          
          //TODO: add _self link
    }
    
    public Stream<T> execute(ImportTaskMetaData<T> task) throws Exception {
                 return fetchAdapterFactory(task)
                         .createAdapter(task.adapterSettings)
                         .parse(payloadService.getPayloadAsInputStream(task.payloadID).get());
    }
    private ImportAdapterFactory<T> fetchAdapterFactory(ImportTaskMetaData<T> task) throws Exception{
                 if(task.adapter==null){
                         throw new IOException("Cannot predict settings with null import adapter");
                 }
                 ImportAdapterFactory<T> adaptFac= (ImportAdapterFactory<T>)  
                         getImportAdapterFactory(task.adapter)
                         .orElse(null);
                 if(adaptFac==null){
                         throw new IOException("Cannot predict settings with unknown import adapter:\"" + task.adapter + "\"");
                 }
                 return adaptFac;
     }
            
          
            

            
    private ImportTaskMetaData<T> predictSettings(ImportTaskMetaData<T> task) throws Exception{
                 ImportAdapterFactory<T> adaptFac=  fetchAdapterFactory(task);
                 Optional<InputStream> iStream =  payloadService.getPayloadAsInputStream(task.payloadID);
                 ImportAdapterStatistics predictedSettings = adaptFac.predictSettings(iStream.get());
                 
                 ImportTaskMetaData<T> newMeta = task.copy();
                 newMeta.adapterSettings=predictedSettings.adapterSettings;
                 newMeta.adapterSchema=predictedSettings.adapterSchema;

                 return newMeta;
     }
    private Optional<Payload> fetchPayload(ImportTaskMetaData task){
                return payloadRepository.findById(task.getPayloadID());
     }
    
    public ImportTaskMetaData from(Payload p){
        ImportTaskMetaData task = new ImportTaskMetaData();
        task.myuuid = UUID.randomUUID();
        task.payloadID=p.id;
        task.size=p.size;
        task.mimeType=p.mimeType;
        task.filename=p.name;
        return task;
    }
    
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //MECHANISM TO SAVE IMPORT META DATA
    //TODO: move to better data store, perhaps more like export/download service
    private Map<UUID,ImportTaskMetaData> importTaskCache = new ConcurrentHashMap<>();
        
    private Optional<ImportTaskMetaData> getImportTask(UUID id){
            return Optional.ofNullable(importTaskCache.get(id));
    }
    
    private Optional<ImportTaskMetaData> saveImportTask(ImportTaskMetaData importTask){
            if(importTask.myuuid ==null){
             importTask.myuuid =UUID.randomUUID();
            }
            importTaskCache.put(importTask.myuuid, importTask);
            return Optional.of(importTask);
    }
    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        
        
    public static interface ImportAdapter<T>{
            public Stream<T> parse(InputStream is);
    }
        
    public static interface ImportAdapterFactory<T>{
		/**
		 * Returns the name of the adapter, to be used for lookups and registration
		 * with a registry of {@link ImportAdapterFactory} possibilities.
		 * @return
		 *   "Key" name of adapter factory.
		 */
		public String getAdapterName();

		/**
		 * Returns list of supported file extensions this import adapter may support. This list
		 * does not have to be exhaustive, but will serve as a hint when suggesting adapters from
		 * a set of possible adapters.
		 * 
		 * @return
		 *   list of supported file extensions
		 */
		public List<String> getSupportedFileExtensions();

		/**
		 * Creates an {@link ImportAdapter} based on the supplied {@link JsonNode}, which can
		 * encode information about the initialization of the adapter. The adapterSettings typically
		 * contain information like how individual fields of an input data stream map to fields in
		 * an output record.
		 * 
		 * @param adapterSettings initialization settings which can be used to configure an {@link ImportAdapter}
		 * @return
		 */
		public ImportAdapter<T> createAdapter(JsonNode adapterSettings);

		/**
		 * <p>
		 * Partially or completely read a given {@link InputStream} and produce {@link ImportAdapterStatistics}
		 * as hints for how to create proper adapterSettings to be used in {@link #createAdapter(JsonNode)}.
		 * </p>
		 * 
		 * <p>
		 *  {@link ImportAdapterStatistics} has 2 intended elements:
		 *  <ol>
		 *  <li>adapterSettings -- a "best guess" set of initialization parameters</li>
		 *  <li>adapterSchema  -- a higher-level model for what input options are available</li>
		 *  </ol>
		 * </p>
		 * 
		 * @param is the InputStream of real or example data to be analyzed
		 * @return 
		 *   {@link ImportAdapterStatistics} object giving some statistics and suggestions for how to configure
		 *   the {@link ImportAdapter} with the {@link #createAdapter(JsonNode)} method.
		 */
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
                               @RequestParam Map<String, String> queryParameters) throws Exception {
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
                    Payload payload = payloadService.createPayload(file.getOriginalFilename(),
                            PayloadController.predictMimeTypeFromFile(file),
                            file.getBytes(),
                            PayloadService.PayloadPersistType.TEMP);
                    return payload.id;
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            Payload payload = payloadRepository.findById(payloadId).get();
            ImportTaskMetaData itmd = from(payload);
            if(adapterName!=null){
                    itmd.setAdapter(adapterName);
            }
            itmd = saveImportTask(itmd).get();
            if(itmd.getAdapter()!=null && itmd.getAdapterSettings() == null){
                   itmd = predictSettings(itmd);
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
        Optional<ImportTaskMetaData> obj = getImportTask(UUID.fromString(id));
        if(obj.isPresent()){
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
        if(obj.isPresent()){
            String adapterName = queryParameters.get("adapter");
            ImportTaskMetaData itmd = obj.get().copy();
            if(adapterName!=null){
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
           ObjectMapper om = new ObjectMapper();
           ImportTaskMetaData itmd = om.treeToValue(updatedJson,ImportTaskMetaData.class);
            
           if(itmd.getAdapter()!=null && itmd.getAdapterSettings() == null){
                   itmd = predictSettings(itmd);
           }
            
           //TODO: validation
           //override any existing task version
           itmd=saveImportTask(itmd).get();
           return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(itmd, queryParameters), HttpStatus.OK);
    }
       
    //STEP 3.5: Preview import
        //!!!!NEEDS MORE WORK
    @hasAdminRole
    @PostGsrsRestApiMapping(value = {"/import({id})/@preview", "/import/{id}/@preview"})
    public ResponseEntity<Object> executePreview(@PathVariable("id") String id,
                                   @RequestParam Map<String, String> queryParameters) throws Exception {
           Optional<ImportTaskMetaData> obj = getImportTask(UUID.fromString(id));
           if(obj.isPresent()){
                //TODO: make async and do other stuff:
                ImportTaskMetaData itmd = obj.get();
                long limit = Long.parseLong(queryParameters.getOrDefault("limit","10"));

                List<T> previewList = (List<T>)(execute(itmd)
                    .limit(limit)
                    .collect(Collectors.toList()));

                return new ResponseEntity<>(GsrsControllerUtil.enhanceWithView(previewList, queryParameters), HttpStatus.OK);
           }
           return gsrsControllerConfiguration.handleNotFound(queryParameters);
    }        
        

    //STEP 4: Execute import
        //!!!!NEEDS MORE WORK
    @hasAdminRole
    @PostGsrsRestApiMapping(value = {"/import({id})/@execute", "/import/{id}/@execute"})
    public ResponseEntity<Object> executeImport(@PathVariable("id") String id,
                                   @RequestParam Map<String, String> queryParameters) throws Exception {
           Optional<ImportTaskMetaData> obj = getImportTask(UUID.fromString(id));
           if(obj.isPresent()){
                //TODO: make async and do other stuff:
                ImportTaskMetaData itmd = obj.get();
                
                execute(itmd)
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
