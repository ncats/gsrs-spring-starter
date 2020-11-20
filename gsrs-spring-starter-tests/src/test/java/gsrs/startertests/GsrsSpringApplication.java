package gsrs.startertests;

import gsrs.EnableGsrsApi;
import gsrs.EnableGsrsJpaEntities;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@EnableConfigurationProperties
@EnableGsrsApi(indexerType = EnableGsrsApi.IndexerType.LEGACY,
                entityProcessorDetector = EnableGsrsApi.EntityProcessorDetector.CUSTOM,
                indexValueMakerDetector = EnableGsrsApi.IndexValueMakerDetector.CUSTOM)
@EnableGsrsJpaEntities
@SpringBootApplication
//@EntityScan(basePackages ={"ix","gsrs", "gov.nih.ncats"} )
@EnableJpaRepositories(basePackages ={"ix","gsrs", "gov.nih.ncats"} )
@Import(GsrsEntityTestConfiguration.class)
public class GsrsSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(GsrsSpringApplication.class, args);
    }

}
