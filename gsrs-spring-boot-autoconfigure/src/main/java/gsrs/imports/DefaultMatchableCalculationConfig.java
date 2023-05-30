package gsrs.imports;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultMatchableCalculationConfig<T> implements MatchableCalculationConfig {

    private Class extractorClass;

    private JsonNode extractorConfig;

    @Override
    public Class getMatchableCalculationClass() {
        return extractorClass;
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
