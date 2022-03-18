package gsrs.dataExchange.model;

import java.util.List;
import java.util.Optional;

public interface SDFRecord {
    public String getStructure();
    public Optional<String> getProperty(String name);
    public List<String> getProperties();
}
