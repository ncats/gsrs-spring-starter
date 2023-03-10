package gsrs.dataexchange.model;

import ix.ginas.models.GinasCommonData;

import java.util.Map;
import java.util.function.Consumer;

/*
Simple processing action, for unit tests.
Kind of a merge
 */
public class BasicProcessingAction implements ProcessingAction<GinasCommonData>{
    @Override
    public GinasCommonData process(GinasCommonData stagingAreaRecord, GinasCommonData additionalRecord, Map parameters, Consumer log) throws Exception {
        //some minimal mods
        if( hasTrueValue(parameters,"copyAccess")) {
            stagingAreaRecord.setAccess(additionalRecord.getAccess());
        }
        if(hasTrueValue(parameters, "copyUuid")){
            stagingAreaRecord.setUuid(additionalRecord.getUuid());
        }
        return stagingAreaRecord;
    }
}
