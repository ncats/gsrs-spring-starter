package gsrs.imports;

import ix.core.util.InheritanceTypeIdResolver;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@InheritanceTypeIdResolver.DefaultInstance
public class ActionConfigImpl implements ActionConfig{
    private String actionName;
    private Class actionClass;
    private List<CodeProcessorFieldImpl> fields;
    private Map<String, Object> parameters = new HashMap<>();

    private String label;

    @Override
    public String getActionName() {
        return this.actionName;
    }

    @Override
    public void setActionName(String actionName) {
        this.actionName =actionName;
    }

    @Override
    public Class getActionClass() {
        return this.actionClass;
    }

    @Override
    public void setActionClass(Class actionClass) {
        this.actionClass =actionClass;
    }

    @Override
    public List<CodeProcessorFieldImpl> getFields() {
        return this.fields;
    }

    @Override
    public void setFields(List<CodeProcessorFieldImpl> fields) {
        this.fields=fields;
    }

    @Override
    public Map<String, Object> getParameters() {
        return this.parameters;
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
        this.parameters= parameters;
    }

    @Override
    public void setLabel(String label){
        this.label= label;
    }

    @Override
    public String getLabel(){
        return this.label;
    }

}
