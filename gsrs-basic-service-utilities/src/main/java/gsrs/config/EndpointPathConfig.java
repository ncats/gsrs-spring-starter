package gsrs.config;

public interface EndpointPathConfig<T> {

    String getParentKey();

    String getName();

    String getPath();

    Double getOrder();

    boolean isDisabled();

    void setParentKey(String parentKey);

    void setName(String name);

    void setPath(String path);

    void setOrder(Double order);

    void setDisabled(boolean disabled);

}
