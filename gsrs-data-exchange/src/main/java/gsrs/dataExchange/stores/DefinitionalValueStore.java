package gsrs.dataExchange.stores;

import gsrs.dataExchange.model.DefinitionalValue;

import java.util.List;
import java.util.stream.Stream;

public interface DefinitionalValueStore {
    public void add(DefinitionalValue df);
    public void add(String id, String key, String val, String qualifier);
    public void removeAll(String id);
    public List<DefinitionalValue> find(DefinitionalValue seek);
    public List<DefinitionalValue> find(String key, String val, String qualifier);
    public List<DefinitionalValue> find(String id);
    public void clearAll();
    public Stream<DefinitionalValue> findAll();
}
