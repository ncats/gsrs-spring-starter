package gsrs.imports;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.InputStream;
import java.util.stream.Stream;

/*
Creates a set of domain entity from data in an InputStream
 */
public interface ImportAdapter<T> {
    Stream<T> parse(InputStream is, ObjectNode settings, JsonNode adapterSchema);
}
