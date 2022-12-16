package gsrs.dataexchange.model;

import java.util.Map;
import java.util.function.Consumer;

/*
Something that the user does to a record in the holding area
 */
public interface ProcessingAction<T> {

    T process(T stagingAreaRecord, T additionalRecord, Map<String, Object> parameters, Consumer<String> log) throws Exception;
}
