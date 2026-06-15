package gsrs.dataexchange;

import gsrs.dataexchange.model.ProcessingActionConfig;
import gsrs.dataexchange.model.ProcessingActionConfigSet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GsrsDataExchangeSmokeTest {

    @Test
    void processingActionConfigSetAddsActionsToDefaultList() {
        ProcessingActionConfigSet configSet = ProcessingActionConfigSet.builder().build();
        ProcessingActionConfig actionConfig = new ProcessingActionConfig();
        actionConfig.setProcessingActionName("normalize");

        configSet.addAction(actionConfig);

        assertNotNull(configSet.getProcessingActions());
        assertEquals(1, configSet.getProcessingActions().size());
        assertEquals("normalize", configSet.getProcessingActions().get(0).getProcessingActionName());
    }
}
