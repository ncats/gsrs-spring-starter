package gsrs.dataExchange.model;

import java.util.Map;

/*
produces a MappingAction based on parameters
 */
public interface MappingActionFactory<T, U> {
    MappingAction<T,U> create(Map<String, Object> params) throws Exception;
    MappingActionFactoryMetadata getMetadata();
    Map<String, Object> getParameters();
    void setParameters(Map<String, Object>  parameters);
    void implementParameters();
}
