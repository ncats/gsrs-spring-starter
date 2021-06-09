package gsrs.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("gsrs.rabbitmq")
@Data
public class GsrsRabbitMqConfiguration {

    private String exchange = "gsrs_exchange";

    boolean enabled = true;
}
