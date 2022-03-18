package gsrs.dataExchange.model;

import java.util.Map;

public interface MappingActionFactory<T, U> {
    public MappingAction<T,U> create(Map<String, Object> params) throws Exception;
    public MappingActionFactoryMetadata getMetadata();
}
