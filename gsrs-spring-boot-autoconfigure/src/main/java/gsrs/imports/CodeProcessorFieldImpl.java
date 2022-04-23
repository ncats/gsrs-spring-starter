package gsrs.imports;

class CodeProcessorFieldImpl implements CodeProcessorField {

    private String fieldName;  // used for identification within code and metadata
    private String fieldLabel;      // display within UI
    private Object defaultValue;
    private Class fieldType;
    private Boolean showInUi;
    private Boolean required;   //required for UI
    private String lookupKey;

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

    @Override
    public String getLookupKey() {
        return lookupKey;
    }

    @Override
    public void setLookupKey(String key) {
        this.lookupKey= key;
    }
}