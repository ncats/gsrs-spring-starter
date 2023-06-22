package gsrs.imports;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

@Data
public class ImportAdapterStatistics {
    private JsonNode adapterSettings;
    private JsonNode adapterSchema;
}
