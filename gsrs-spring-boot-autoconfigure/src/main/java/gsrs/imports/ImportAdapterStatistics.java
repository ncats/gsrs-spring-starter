package gsrs.imports;

import tools.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class ImportAdapterStatistics {
    private JsonNode adapterSettings;
    private JsonNode adapterSchema;
}
