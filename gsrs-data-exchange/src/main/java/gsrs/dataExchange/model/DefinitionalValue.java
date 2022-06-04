package gsrs.dataExchange.model;

import ix.core.models.Indexable;
import ix.core.models.IndexableRoot;
import lombok.Data;

@Data
@IndexableRoot
public class DefinitionalValue {

    @Indexable(name="Id")
    private String id;

    @Indexable(name="Key")
    private String key;

    @Indexable(name="Value")
    private String value;

    @Indexable(name="Qualifier")
    private String qualifier;
}
