package gsrs.startertests;

import gsrs.DefaultDataSourceConfig;
import gsrs.EnableGsrsApi;
import gsrs.EnableGsrsBackup;
import gsrs.EnableGsrsJpaEntities;
import gsrs.GsrsFactoryConfiguration;
import gsrs.repository.UserProfileRepository;

import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

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
