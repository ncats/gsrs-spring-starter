package dataExchange.imports;

import gsrs.dataExchange.model.MappingAction;
import ix.ginas.models.GinasCommonData;

public class Item1MappingAction implements MappingAction<GinasCommonData, TextRecordContext> {
    @Override
    public GinasCommonData act(GinasCommonData building, TextRecordContext source) {
        if( source.getItem1()  !=null && source.getItem1().trim().length() >0) {
            building.addMatchContextProperty("Item 1", source.getItem1());
        }
        return building;
    }
}
