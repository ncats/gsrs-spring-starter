package gsrs.imports;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.List;
import java.util.Map;

/*
information necessary to set up a MappingAction
 */
public interface ActionConfig {
    String getActionName();
    void setActionName(String actionName);

    Class getActionClass();
    void setActionClass(Class actionClass);

    List<CodeProcessorFieldImpl> getFields();
    void setFields(List<CodeProcessorFieldImpl> fields);

    //parameters allow configuration of a generic action, for example,
    // a generic code handler that handles multiple code systems.
    // we can specify a code system as a parameter
    Map<String,Object> getParameters();
    void setParameters(Map<String,Object> parameters);
}
