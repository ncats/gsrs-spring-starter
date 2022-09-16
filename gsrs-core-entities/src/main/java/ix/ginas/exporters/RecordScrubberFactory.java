package ix.ginas.exporters;


import com.fasterxml.jackson.databind.JsonNode;

public interface RecordScrubberFactory<T> {

    RecordScrubber<T> createScrubber(JsonNode settings);
}
