package gsrs.dataexchange.model;

import ix.ginas.models.GinasCommonData;

import java.util.Map;
import java.util.function.Consumer;

/*
Simple processing action, for unit tests
 */
public class OriginalProcessingAction implements ProcessingAction<GinasCommonData>{
    @Override
    public GinasCommonData process(GinasCommonData stagingAreaRecord, GinasCommonData additionalRecord, Map<String, Object> parameters, Consumer<String> log) throws Exception {
        if( hasTrueValue(parameters, "returnStagingRecord"))  return stagingAreaRecord;
        return additionalRecord;
    }
}
