package gsrs.imports;

class CodeProcessorFieldImpl implements CodeProcessorField {

    private String fieldName;
    private String fieldLabel;
    private Object defaultValue;
    private Class fieldType;
    private Boolean showInUi;
    private Boolean required;

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public void setFieldName(String fieldName) {
        this.fieldName =fieldName;
    }

    @Override
    public String getFieldLabel() {
        return this.fieldLabel;
    }

    @Override
    public void setFieldLabel(String fieldLabel) {
        this.fieldLabel= fieldLabel;
    }

    @Override
    public Class getFieldType() {
        return this.fieldType;
    }

    @Override
    public void setFieldType(Class fieldType) {
        this.fieldType=fieldType;
    }

    @Override
    public Boolean getRequired() {
        return this.required;
    }

    @Override
    public void setRequired(Boolean required) {
        this.required=required;
    }

    @Override
    public Object getDefaultValue() {
        return this.defaultValue;
    }

    @Override
    public void setDefaultValue(Object defaultValue) {
        this.defaultValue=defaultValue;
    }

    @Override
    public Boolean isShowInUi() {
        return this.showInUi;
    }

    @Override
    public void setShowInUi(Boolean showInUi) {
        this.showInUi=showInUi;
    }
}