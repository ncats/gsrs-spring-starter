package gsrs.dataExchange.model;

import java.util.List;
import java.util.Optional;

public interface SDFRecord {
    String getStructure();
    Optional<String> getProperty(String name);
    List<String> getProperties();
}
