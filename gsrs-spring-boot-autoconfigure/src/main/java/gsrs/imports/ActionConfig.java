package gsrs.imports;

import java.util.List;

public interface ActionConfig {
    String getActionName();
    void setActionName(String actionName);

    Class getActionClass();
    void setActionClass(Class actionClass);

    List<CodeProcessorField> getFields();
    void setFields(List<CodeProcessorField> fields);
}
