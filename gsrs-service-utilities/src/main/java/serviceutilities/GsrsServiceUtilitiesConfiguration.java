package serviceutilities;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GsrsServiceUtilitiesConfiguration {
    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public String testing() {
        String s = "I am here  ...... XXX ... XXX";
        System.out.println();
        return s;
    }
}
