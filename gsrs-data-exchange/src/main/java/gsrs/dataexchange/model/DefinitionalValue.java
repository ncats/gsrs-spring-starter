package gsrs.dataexchange.model;

import ix.core.models.Indexable;
import ix.core.models.IndexableRoot;
import lombok.Data;

@Data
@IndexableRoot
public class DefinitionalValue {

    @Indexable(name="Id")
    private String id;

    @Indexable(name="DefinitionalKey")
    private String key;

    @Indexable(name="DefinitionalValue")
    private String value;

    @Indexable(name="Qualifier")
    private String qualifier;

}
