package gsrs;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties("ix.json")
@Data
public class JsonTypeIdResolverConfiguration {

    private List<String> typeIdResolvers;


}
