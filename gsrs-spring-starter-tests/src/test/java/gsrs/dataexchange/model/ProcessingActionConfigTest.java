package gsrs.dataexchange.model;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.Map;

public class ProcessingActionConfigTest {

    @Test
    public void ConfigSerializationTest() {
        ProcessingActionConfigSet configSet= new ProcessingActionConfigSet();
        ProcessingActionConfig config = new ProcessingActionConfig();
        config.setProcessingActionName(BasicProcessingAction.class.getName());
        Map<String, Object> parms = new HashMap<>();
        parms.put("copyAccess", true);
        parms.put("copyUuid", false);
        config.setParameters(parms);
        configSet.addAction(config);

        ProcessingActionConfig config2 = new ProcessingActionConfig();
        config2.setProcessingActionName(OriginalProcessingAction.class.getName());
        Map<String, Object> parms2 = new HashMap<>();
        parms2.put("returnStagingRecord", true);
        config2.setParameters(parms2);
        configSet.addAction(config2);

        JsonMapper mapper = JsonMapper.builderWithJackson2Defaults().build();
        String json = mapper.writeValueAsString(configSet);
        System.out.println(json);
        Assertions.assertTrue(json.length()>0);
    }

    @Test
    /*
    testing deserialization
     */
    public void ConfigDeserializationTest() {
        String inputJson ="{\"processingActions\":[{\"parameters\":{\"copyUuid\":false,\"copyAccess\":false},\"processingActionName\":\"gsrs.dataexchange.model.BasicProcessingAction\"},{\"parameters\":{\"returnStagingRecord\":false},\"processingActionName\":\"Original\"}]}";
        JsonMapper mapper= JsonMapper.builderWithJackson2Defaults().build();
        ProcessingActionConfigSet configSet =mapper.readValue(inputJson, ProcessingActionConfigSet.class);
        Assertions.assertEquals(2, configSet.processingActions.size());
        Assertions.assertTrue( configSet.getProcessingActions().stream().allMatch(c->c.getParameters().keySet().stream().allMatch(p->c.getParameters().get(p).getClass().getName().contains("Boolean"))));
    }
}
