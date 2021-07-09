package gsrs.controller.hateoas;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@Data
public class IxContext {
    
    @Value("${application.host:#{null}}")
    private String host;

    
}
