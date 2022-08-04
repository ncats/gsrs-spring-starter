package gsrs.dataexchange.imports;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.dataexchange.model.MappingAction;
import gsrs.imports.ImportAdapter;
import gsrs.imports.ImportAdapterFactory;
import gsrs.imports.ImportAdapterStatistics;
import ix.ginas.models.GinasCommonData;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
public class BasicImportFactory implements ImportAdapterFactory<GinasCommonData> {
    private String fileName;
    private Class holdingAreaService;
    private List<Class> services;
    private Class serviceClass;

    @Override
    public String getAdapterName() {
        return "Basic Import Adapter";
    }

    @Override
    public List<String> getSupportedFileExtensions() {
        return Arrays.asList(new String[] {"txt", "text"});
    }

    @Override
    public ImportAdapter<GinasCommonData> createAdapter(JsonNode adapterSettings) {
        log.trace("starting in createAdapter. adapterSettings: " + adapterSettings.toPrettyString());
        List<MappingAction<GinasCommonData, TextRecordContext>> actions = null;
        try {
            actions = getMappingActions(adapterSettings);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new BasicTextFileImporter(actions);

    }

    @Override
    public ImportAdapterStatistics predictSettings(InputStream is) {
        return null;
    }

    @Override
    public void setFileName(String fileName) {
        this.fileName=fileName;
    }

    @Override
    public String getFileName() {
        return this.fileName;
    }

    @Override
    public void setHoldingAreaService(Class newClass) {
        this.holdingAreaService= newClass;
    }

    @Override
    public Class getHoldingAreaEntityService() {
        return null;
    }

    @Override
    public void setHoldingAreaEntityService(Class holdingAreaEntityService) {

    }

    @Override
    public Class getHoldingAreaService(){
        return this.holdingAreaService;
    }

    public static List<MappingAction<GinasCommonData, TextRecordContext>> getMappingActions(JsonNode adapterSettings) throws Exception {
        List<MappingAction<GinasCommonData, TextRecordContext>> actions = new ArrayList<>();
        log.trace("adapterSettings: " + adapterSettings.toPrettyString());
        adapterSettings.get("actions").forEach(js -> {
            String actionName = js.get("actionName").asText();
            JsonNode actionParameters = js.get("actionParameters");
            System.out.println("actionParameters: "+actionParameters.toPrettyString());
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> params = mapper.convertValue(actionParameters, new TypeReference<Map<String, Object>>() {
            });
            MappingAction<GinasCommonData, TextRecordContext> action =null;
            try {
                String actionClassName="";
                JsonNode actionClassNode = js.get("actionClassName");
                if(actionClassNode!=null ) actionClassName=actionClassNode.asText();
                if(actionClassName!=null && actionClassName.length()>0) {
                    log.trace("going to instantiate class of type " + actionClassName);
                    Class actionClass= Class.forName(actionClassName);
                    action= (MappingAction) actionClass.newInstance();
                    log.trace("class instantiated");
                }
                else {
                    log.trace("using default class");
                    action=new BasicMappingAction();
                }
            }
            catch (Exception ex) {
                log.error("Error in getMappingActions: " + ex.getMessage());
                ex.printStackTrace();
            }

            actions.add(action);
        });
        return actions;
    }

    @Override
    public List<Class> getEntityServices() {
        return this.services;
    }

    @Override
    public void setEntityServices(List<Class> services) {
        this.services=services;
    }

    @Override
    public Class getEntityServiceClass() {
        return this.serviceClass;
    }

    @Override
    public void setEntityServiceClass(Class newClass) {
        this.serviceClass=newClass;
    }
}
