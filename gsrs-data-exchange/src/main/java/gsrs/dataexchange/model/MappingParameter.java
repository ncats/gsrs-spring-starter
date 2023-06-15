package gsrs.dataexchange.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MappingParameter<T> {
    private String fieldName;
    private String label;
    private boolean required = false;
    private T defaultValue;
    private Class<T> valueType;
    //todo: discuss usefulness of this field:
    private boolean expectedToChange =true;
    //todo: discuss usefulness of this field:
    private String lookupKey;

    public String getLookupKey() {
        return (lookupKey!=null && lookupKey.length()>0) ? lookupKey : fieldName;
    }

    public MappingParameter(MappingParameterBuilder builder) {
        this.fieldName = builder.getFieldName();
        this.label = builder.getLabel();
        this.required = builder.isRequired();
        this.defaultValue = (T) builder.getDefaultValue();
        this.valueType = builder.getValueType();
        this.expectedToChange = builder.isExpectedToChange();
        this.lookupKey=builder.getLookupKey();
    }

    public static MappingParameterBuilder builder() {
        return new MappingParameterBuilder();
    }

}
