package gsrs.util;

public interface ExtensionConfig<T> {

    String getParentKey();

    Double getOrder();

    boolean isDisabled();

    void setParentKey(String parentKey);

    void setOrder(Double order);

    void setDisabled(boolean disabled);

}
