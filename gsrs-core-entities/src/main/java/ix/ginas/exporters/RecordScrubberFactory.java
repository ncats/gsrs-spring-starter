package ix.ginas.exporters;


import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;

public interface RecordScrubberFactory<T> {

    RecordScrubber<T> createScrubber(JsonNode settings);

    default JsonNode getSettingsSchema(){
        return JsonNodeFactory.instance.objectNode(); //should be some default very permissive schema really
    }
}
