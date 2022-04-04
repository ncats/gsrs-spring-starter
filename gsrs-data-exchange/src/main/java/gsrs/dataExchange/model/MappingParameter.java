package gsrs.dataExchange.model;

import lombok.Data;

@Data
public class MappingParameter<T> {
    private String fieldName;
    private String label;
    private boolean required = false;
    private T defaultValue;
    private Class<T> valueType;
    private boolean displayInUI=true;

    public MappingParameter(MappingParameterBuilder builder) {
        this.fieldName = builder.getFieldName();
        this.label = builder.getLabel();
        this.required = builder.isRequired();
        this.defaultValue = (T) builder.getDefaultValue();
        this.valueType = builder.getValueType();
        this.displayInUI= builder.isDisplayInUI();
    }

    public static MappingParameterBuilder builder() {
        return new MappingParameterBuilder();
    }

}
