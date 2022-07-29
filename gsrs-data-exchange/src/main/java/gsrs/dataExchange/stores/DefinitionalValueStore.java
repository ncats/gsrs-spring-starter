package gsrs.dataExchange.stores;

import gsrs.dataExchange.model.DefinitionalValue;

import java.util.List;
import java.util.stream.Stream;

public interface DefinitionalValueStore {
    void add(DefinitionalValue df);
    void add(String id, String key, String val, String qualifier);
    void removeAll(String id);
    List<DefinitionalValue> find(DefinitionalValue seek);
    List<DefinitionalValue> find(String key, String val, String qualifier);
    List<DefinitionalValue> find(String id);
    void clearAll();
    Stream<DefinitionalValue> findAll();
}
