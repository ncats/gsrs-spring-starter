package gsrs.dataExchange.model;

/*
adds something to a domain object of class T
 */
public interface MappingAction<T, U>{
    public T act(T building, U source) throws Exception;
}
