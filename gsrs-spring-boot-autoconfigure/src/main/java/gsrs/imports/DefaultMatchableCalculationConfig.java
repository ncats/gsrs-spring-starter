package gsrs.imports;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultMatchableCalculationConfig<T> implements MatchableCalculationConfig {

    private Class extractorClass;

    private String parentKey;
    private Double order;
    private boolean disabled = false;

    private JsonNode extractorConfig;

    @Override
    public Class getMatchableCalculationClass() {
        return extractorClass;
    }

    @Override
    public String getParentKey() {
        return parentKey;
    }

    @Override
    public void setParentKey(String parentKey) {
        this.parentKey = parentKey;
    }

    @Override
    public Double getOrder() {
        return order;
    }

    @Override
    public void setOrder(Double order) {
        this.order = order;
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }


    @Override
    public void setMatchableCalculationClass(Class calculatorClass) {
        this.extractorClass=calculatorClass;
    }

    @Override
    public void setConfig(JsonNode config) {
        this.extractorConfig=config;
    }

    @Override
    public JsonNode getConfig(){
        return  this.extractorConfig;
    }
}
