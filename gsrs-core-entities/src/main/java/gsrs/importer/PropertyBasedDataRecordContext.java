package gsrs.importer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PropertyBasedDataRecordContext extends BaseDataRecordContext {

    Optional<String> getProperty(String name);

    List<String> getProperties();

    Map<String, String> getSpecialPropertyMap();

    default Optional<String> resolveSpecial(String name){
        if (name.startsWith("UUID_")) {
            String ret = getSpecialPropertyMap().computeIfAbsent(name, k -> {
                return UUID.randomUUID().toString();
            });
            return Optional.ofNullable(ret);
        }
        return Optional.empty();
    }

}
