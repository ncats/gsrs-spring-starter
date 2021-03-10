package gsrs;


import gsrs.repository.PrincipalRepository;
import gsrs.services.PrincipalService;
import gsrs.services.PrincipalServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(JpaRepositoriesAutoConfiguration.class)
@Import(StarterEntityRegistrar.class)
public class GsrsEntitiesConfiguration {


    @Bean
    @ConditionalOnMissingBean
    PrincipalService principalService(PrincipalRepository principalRepository){
        return new PrincipalServiceImpl(principalRepository);
    }

}
