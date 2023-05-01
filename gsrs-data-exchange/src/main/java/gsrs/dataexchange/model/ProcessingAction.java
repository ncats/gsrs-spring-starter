package gsrs.dataexchange.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/*
Something that the user does to a record in the staging area
 */
public interface ProcessingAction<T> {

    T process(T stagingAreaRecord, T additionalRecord, Map<String, Object> parameters, Consumer<String> log) throws Exception;

    default boolean hasTrueValue(Map<String, Object> parameters, String parameterName) {
        if (parameters.get(parameterName) == null) {
            return false;
        }
        if (parameters.get(parameterName) instanceof Boolean) {
            return (Boolean) parameters.get(parameterName);
        }
        return (parameters.get(parameterName).toString().equalsIgnoreCase("true"));
    }

    default boolean hasStringValue(Map<String, Object> parameters, String parameterName) {
        if (parameters.get(parameterName) == null) {
            return false;
        }
        if (parameters.get(parameterName) instanceof String) {
            return ((String) parameters.get(parameterName)).length()>0;
        }
        return (parameters.get(parameterName).toString().equalsIgnoreCase("true"));
    }
    String getActionName();

    default List<String> getOptions(){
        return Collections.EMPTY_LIST;
    }

    default JsonNode getAvailableSettingsSchema(){
        return JsonNodeFactory.instance.objectNode(); //should be some default very permissive schema really
    }
}
