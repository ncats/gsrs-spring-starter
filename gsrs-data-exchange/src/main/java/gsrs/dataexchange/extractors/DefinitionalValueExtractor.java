package gsrs.dataexchange.extractors;

import gsrs.dataexchange.model.DefinitionalValue;

import java.util.ArrayList;
import java.util.List;

public interface DefinitionalValueExtractor<T> {
    List<DefinitionalValue> extract(T obj);

    default DefinitionalValueExtractor<T> combine(DefinitionalValueExtractor<T> another){

        return (t)->{
            List<DefinitionalValue> list1 = new ArrayList<>(this.extract(t));
            list1.addAll(another.extract(t));
            return list1;
        };
    }
}
