package ix.ginas.exporters;

import tools.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class JsonNodeScrubberExportSetting implements ScrubberExportSettings{

    private JsonNode settings;

    public JsonNodeScrubberExportSetting(JsonNode node) {
        this.settings= node;
    }

}
