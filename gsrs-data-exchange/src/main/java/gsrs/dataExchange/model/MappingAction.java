package gsrs.dataExchange.model;

public interface MappingAction<T, U>{
    public T act(T building, U source) throws Exception;
}
