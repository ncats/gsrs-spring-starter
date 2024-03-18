package gsrs.controller;

import gsrs.GsrsFactoryConfiguration;
import gsrs.validator.ValidatorConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ExtensionConfigurationController {

    @Autowired
    GsrsFactoryConfiguration gsrsFactoryConfiguration;

    @GetMapping("/api/v1/validatorsConfiguration")
    public ResponseEntity<List<? extends ValidatorConfig> > getValidators() {
        List<? extends ValidatorConfig> validatorConfigs = gsrsFactoryConfiguration.getValidatorConfigByContext("substances");
        System.out.println(validatorConfigs.toString());
        return (ResponseEntity<List<? extends ValidatorConfig>>) validatorConfigs;
    }
    @GetMapping("/api/v1/someText")
    public String getSomeText() {
        return "Hi there";
    }

}
