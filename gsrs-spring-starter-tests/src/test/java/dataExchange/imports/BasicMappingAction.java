package dataExchange.imports;

import gsrs.dataExchange.model.MappingAction;
import ix.ginas.models.GinasCommonData;

public class BasicMappingAction implements MappingAction<GinasCommonData, TextRecordContext> {
    @Override
    public GinasCommonData act(GinasCommonData building, TextRecordContext source) {
        //do nothing here; the Adapter will do the work for this basic example
        return null;
    }

}
