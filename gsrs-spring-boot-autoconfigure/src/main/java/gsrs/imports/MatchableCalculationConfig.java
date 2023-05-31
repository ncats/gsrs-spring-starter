package gsrs.imports;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import gsrs.stagingarea.model.MatchableKeyValueTupleExtractor;
import ix.core.util.InheritanceTypeIdResolver;

/*
Support handling matchable calculation configuration - setting the class + properties within JSON
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "configClass", defaultImpl = DefaultMatchableCalculationConfig.class)
@JsonTypeIdResolver(InheritanceTypeIdResolver.class)
public interface MatchableCalculationConfig<T> {

    Class<? extends MatchableKeyValueTupleExtractor> getMatchableCalculationClass();

    void setMatchableCalculationClass(Class<? extends MatchableKeyValueTupleExtractor> calculatorClass);

    void setConfig(JsonNode config);

    JsonNode getConfig();
}
