package gsrs.imports;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import gsrs.stagingarea.model.MatchableKeyValueTupleExtractor;
import ix.core.util.InheritanceTypeIdResolver;

/**
Description of set-up of matchable calculations - the class + properties within JSON
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "configClass", defaultImpl = DefaultMatchableCalculationConfig.class)
@JsonTypeIdResolver(InheritanceTypeIdResolver.class)
public interface MatchableCalculationConfig<T> {

    Class<? extends MatchableKeyValueTupleExtractor> getMatchableCalculationClass();

    String getKey();
    void setKey(String key);

    Double getOrder();
    void setOrder(Double order);

    boolean isDisabled();
    void setDisabled(boolean disabled);

    void setMatchableCalculationClass(Class<? extends MatchableKeyValueTupleExtractor> calculatorClass);

    void setConfig(JsonNode config);

    JsonNode getConfig();
}
