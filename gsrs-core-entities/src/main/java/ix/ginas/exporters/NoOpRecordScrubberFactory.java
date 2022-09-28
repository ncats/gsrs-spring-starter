package ix.ginas.exporters;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

public class NoOpRecordScrubberFactory<T>  implements RecordScrubberFactory<T> {

    @Override
    public RecordScrubber<T> createScrubber(JsonNode settings) {
        //'identity' scrubber returns what was put in
        RecordScrubber<T> identityScrubber = (t)-> Optional.of(t);
        return identityScrubber;
    }
}
