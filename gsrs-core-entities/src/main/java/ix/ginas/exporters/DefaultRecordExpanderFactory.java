package ix.ginas.exporters;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.stream.Stream;

public class DefaultRecordExpanderFactory<T> implements RecordExpanderFactory<T> {

    @Override
    public RecordExpander<T> createExpander(JsonNode settings) {
        RecordExpander<T> identityExpander = ( t)-> Stream.of(t);
        return identityExpander;
    }
}
