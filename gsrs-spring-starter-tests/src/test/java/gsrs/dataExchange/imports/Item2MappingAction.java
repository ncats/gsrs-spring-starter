package gsrs.dataExchange.imports;

import gsrs.dataExchange.model.MappingAction;
import ix.ginas.models.GinasCommonData;

public class Item2MappingAction implements MappingAction<GinasCommonData, TextRecordContext> {

    @Override
    public GinasCommonData act(GinasCommonData building, TextRecordContext source) {
        if( source.getItem2()  !=null && source.getItem2().trim().length() >0) {
            building.addMatchContextProperty("Item 2", source.getItem2());
        }
        return building;
    }
}
