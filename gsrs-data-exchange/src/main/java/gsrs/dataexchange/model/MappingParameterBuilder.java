package gsrs.dataexchange.model;

import lombok.Data;

@Data
public class MappingParameterBuilder<T> {
    private String fieldName;
    private String label;
    private boolean required = false;
    private T defaultValue;
    private Class<T> valueType;
    private boolean expectedToChange =true;
    private String lookupKey;

    public MappingParameterBuilder setFieldName(String fieldName) {
        this.fieldName = fieldName;
        return this;
    }

    public MappingParameterBuilder setLabel(String label) {
        this.label = label;
        return this;
    }

    public MappingParameterBuilder setRequired(boolean r) {
        this.required = r;
        return this;
    }

    public MappingParameterBuilder setDefaultValue(T defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public MappingParameterBuilder setValueType(Class<T> t) {
        this.valueType = t;
        return this;
    }

    public MappingParameterBuilder setExpectedToChange(boolean displayOn) {
        this.expectedToChange = displayOn;
        return this;
    }
    public MappingParameter build() {
        return new MappingParameter(this);
    }

    public static MappingParameterBuilder instance() {
        return new MappingParameterBuilder();
    }

    public MappingParameterBuilder setFieldNameAndLabel(String fieldName, String fieldLabel) {
        this.fieldName = fieldName;
        this.label = fieldLabel;
        return this;
    }

    public MappingParameterBuilder setLookupKey(String lookupKey) {
        this.lookupKey=lookupKey;
        return this;
    }

    protected MappingParameterBuilder() {
    }

}
