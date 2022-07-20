package gsrs.dataExchange.startertests;

import gsrs.EnableGsrsApi;
import gsrs.EnableGsrsBackup;
import gsrs.EnableGsrsJpaEntities;
import gsrs.repository.UserProfileRepository;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@ConditionalOnMissingBean(UserProfileRepository.class)
@EnableConfigurationProperties
@EnableGsrsApi(indexerType = EnableGsrsApi.IndexerType.LEGACY,
        entityProcessorDetector = EnableGsrsApi.EntityProcessorDetector.CUSTOM,
        indexValueMakerDetector = EnableGsrsApi.IndexValueMakerDetector.CUSTOM)
@EnableGsrsJpaEntities
@SpringBootApplication
@EntityScan(basePackages ={"ix","gsrs", "gov.nih.ncats"} )
//@Import(DefaultDataSourceConfig.class)
//@EnableJpaRepositories(basePackages ={"ix","gsrs", "gov.nih.ncats"} )
@EnableGsrsBackup
//@Import(GsrsEntityTestConfiguration.class)
public class GsrsSpringApplication {

    static {
        System.out.println("Launching");
    }

    //    @Bean
//    @ConfigurationProperties("gsrs")
//    public GsrsFactoryConfiguration gsrsFactoryConfiguration(){
//        return new GsrsFactoryConfiguration();
//    }
    public static void main(String[] args) {
        SpringApplication.run(GsrsSpringApplication.class, args);
    }

}
