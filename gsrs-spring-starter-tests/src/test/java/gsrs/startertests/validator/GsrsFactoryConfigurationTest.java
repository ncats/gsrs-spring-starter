package gsrs.startertests.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.GsrsFactoryConfiguration;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class GsrsFactoryConfigurationTest {
    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
            "gsrs.substances.validators.IgnoreValidator.priority=10",
            "gsrs.substances.validators.IgnoreValidator.disabled=false",
            "gsrs.substances.validators.IgnoreValidator.validatorClass=ix.ginas.utils.validation.validators.IgnoreValidator",
            "gsrs.substances.validators.IgnoreValidator.newObjClass=ix.ginas.models.v1.Substance",
            "gsrs.substances.validators.IgnoreValidator.oldObjClass=ix.ginas.models.v1.Substance",
            "gsrs.substances.validators.IgnoreValidator.configClass=SubstanceValidatorConfig"
          ).applyTo(context);
        }
    }
    private ObjectMapper mapper = new ObjectMapper();

//    @Autowired
//    private GsrsFactoryConfiguration gsrsFactoryConfiguration;

    /*
gsrs.validators = {
"substances": {
    "IgnoreValidator": {
         "priority" : 10,
         "disabled": false,
         "validatorClass" = "ix.ginas.utils.validation.validators.IgnoreValidator",
         "newObjClass" = "ix.ginas.models.v1.Substance",
         "configClass" = "SubstanceValidatorConfig"
       }
*/


    @Test
    public void validatorsTest() throws ClassNotFoundException {
        Map<String, Map<String, Map<String, Object>>> map1 = new HashMap<>();
        Map<String, Map<String, Object>> map2 = new HashMap<>();
        Map<String, Object> map3 = new HashMap<>();

        map3.put("priority", 10);
        map3.put("disabled", false);
        map3.put("validatorClass", "ix.ginas.utils.validation.validators.IgnoreValidator");
        map3.put("newObjClass", "ix.ginas.models.v1.Substance");
        map3.put("configClass", "SubstanceValidatorConfig");

        map2.put("IgnoreValidator", map3);
        map1.put("substances", map2);
        GsrsFactoryConfiguration gfc = new GsrsFactoryConfiguration();
        Map<String, Map<String, Map<String, Object>>> map4 = new HashMap<>();

        map4 = gfc.getValidators();
System.out.println(map4.entrySet().size());
    }
}