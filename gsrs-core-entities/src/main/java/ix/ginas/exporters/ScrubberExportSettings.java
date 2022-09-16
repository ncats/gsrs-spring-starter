package ix.ginas.exporters;

import com.fasterxml.jackson.databind.JsonNode;

public interface ScrubberExportSettings {

    static JsonNodeScrubberExportSetting fromJson(JsonNode node){
        return new JsonNodeScrubberExportSetting(node);
    }
}
