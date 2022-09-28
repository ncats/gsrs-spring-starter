package ix.ginas.exporters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public interface RecordExpanderFactory<T> {
    RecordExpander<T> createExpander(JsonNode settings);

    default JsonNode getSettingsSchema(){
        return JsonNodeFactory.instance.objectNode();
    }
}
