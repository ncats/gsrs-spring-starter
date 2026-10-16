package ix.ginas.exporters;


import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

public interface RecordExpanderFactory<T> {
    RecordExpander<T> createExpander(JsonNode settings);

    default JsonNode getSettingsSchema(){
        return JsonNodeFactory.instance.objectNode();
    }
}
