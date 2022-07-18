package gsrs.holdingArea.model;

import lombok.Data;

/*
For Query
Not for persistence
 */
@Data
public class DefinitionalValueTuple {

    private String key;
    private String value;
    private String qualifier;
}
