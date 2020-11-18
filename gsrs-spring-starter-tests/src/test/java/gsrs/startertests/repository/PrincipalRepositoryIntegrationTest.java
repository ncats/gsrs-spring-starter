package gsrs.startertests.repository;


import gsrs.AuditConfig;
import gsrs.BasicEntityProcessorFactory;
import gsrs.EntityProcessorFactory;
import gsrs.GsrsFactoryConfiguration;
import gsrs.repository.PrincipalRepository;
import gsrs.services.PrincipalService;
import gsrs.services.PrincipalServiceImpl;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.ClearAuditorRule;
import gsrs.startertests.ClearTextIndexerRule;
import gsrs.startertests.GsrsSpringApplication;
import ix.core.models.Principal;
import ix.core.search.text.Lucene4IndexServiceFactory;
import ix.core.search.text.TextIndexerConfig;
import ix.core.search.text.TextIndexerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ContextConfiguration(classes = {GsrsSpringApplication.class, PrincipalRepository.class,
        GsrsFactoryConfiguration.class,
        BasicEntityProcessorFactory.class, TextIndexerFactory.class, TextIndexerConfig.class,
        Lucene4IndexServiceFactory.class})

@DataJpaTest
@ActiveProfiles("test")
@Import({ClearAuditorRule.class , ClearTextIndexerRule.class,  AuditConfig.class, AutowireHelper.class})
public class PrincipalRepositoryIntegrationTest {
//    @TestConfiguration
//    public static class Config{
//        @Bean
//        public EntityProcessorFactory entityProcessorFactory(){
//            return new BasicEntityProcessorFactory();
//        }
//    }

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PrincipalRepository repository;



    private PrincipalService principalService;

    @BeforeEach
    public void setup(){
        //have to hardcode this dependency because a DataTest which mocks out
        //the database with an in memory h2 doesn't do service scans etc to find and autowire this service!
        principalService = new PrincipalServiceImpl(repository);
    }
    @Test
    public void findByUsername(){
        String username = "myUsername";
        Principal expected = new Principal(username, null);

        entityManager.persistAndFlush(expected);

        Principal actual= repository.findDistinctByUsernameIgnoreCase(username);
        assertThat(actual).isNotNull();
        assertThat(actual.username).isEqualTo(username);

        assertThat(actual.id).isEqualTo(expected.id);
        assertThat(expected.id).isNotNull();
    }

    @Test
    public void ensureUsernamesAreCaseInsensitive() {

        Principal p1= principalService.registerIfAbsent(new Principal("TEST",null));
        Principal p2=principalService.registerIfAbsent(new Principal("test",null));


        assertThat(p1.id).isNotNull();
        assertThat(p1.id).isEqualTo(p2.id);
    }

}
