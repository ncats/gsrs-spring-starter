package gsrs.imports;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ActionConfigImpl implements ActionConfig{
    private String actionName;
    private Class actionClass;
    private List<CodeProcessorField> fields;
    private Map<String, Object> parameters = new HashMap<>();

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
    public List<CodeProcessorField> getFields() {
        return this.fields;
    }

    @Override
    public void setFields(List<CodeProcessorField> fields) {
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
}
