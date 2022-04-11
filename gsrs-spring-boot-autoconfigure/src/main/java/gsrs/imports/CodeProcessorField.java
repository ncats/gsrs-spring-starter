package gsrs.imports;

public interface CodeProcessorField {

    String getFieldName();
    void setFieldName(String fieldName);

    String getFieldLabel();
    void setFieldLabel(String fieldLabel);

    Class getFieldType();
    void setFieldType(Class fieldType);

    Boolean getRequired();
    void setRequired(Boolean required);

    Object getDefaultValue();
    void setDefaultValue(Object defaultValue);

    Boolean isShowInUi();
    void setShowInUi(Boolean showInUi);

    }
