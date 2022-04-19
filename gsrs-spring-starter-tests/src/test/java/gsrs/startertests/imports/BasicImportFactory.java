package gsrs.startertests.imports;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import gsrs.controller.AbstractImportSupportingGsrsEntityController;
import gsrs.dataExchange.model.MappingAction;
import gsrs.imports.ImportAdapterFactory;
import ix.ginas.models.GinasCommonData;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
public class BasicImportFactory implements ImportAdapterFactory<GinasCommonData> {
    @Override
    public String getAdapterName() {
        return "Basic Import Adapter";
    }

    @Override
    public List<String> getSupportedFileExtensions() {
        return Arrays.asList(new String[] {"txt", "text"});
    }

    @Override
    public AbstractImportSupportingGsrsEntityController.ImportAdapter<GinasCommonData> createAdapter(JsonNode adapterSettings) {
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
    public AbstractImportSupportingGsrsEntityController.ImportAdapterStatistics predictSettings(InputStream is) {
        return null;
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

}
