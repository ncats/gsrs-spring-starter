package ix.ginas.exporters;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public interface RecordScrubberFactory<T> {

    RecordScrubber<T> createScrubber(JsonNode settings);

    default JsonNode getSettingsSchema(){
        return JsonNodeFactory.instance.objectNode(); //should be some default very permissive schema really
    }
}
