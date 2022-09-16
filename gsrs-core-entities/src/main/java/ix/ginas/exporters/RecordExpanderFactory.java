package ix.ginas.exporters;

import com.fasterxml.jackson.databind.JsonNode;

public interface RecordExpanderFactory<T> {
    RecordExpander<T> createExpander(JsonNode settings);
}
