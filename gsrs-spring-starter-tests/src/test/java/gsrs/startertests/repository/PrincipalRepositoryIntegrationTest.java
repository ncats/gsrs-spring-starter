package gsrs.startertests.repository;



import gsrs.repository.PrincipalRepository;
import gsrs.services.PrincipalService;
import gsrs.services.PrincipalServiceImpl;
import gsrs.startertests.*;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.models.Principal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@GsrsJpaTest
@ActiveProfiles("test")
public class PrincipalRepositoryIntegrationTest extends AbstractGsrsJpaEntityJunit5Test {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PrincipalRepository repository;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    private PrincipalService principalService;

    @BeforeEach
    public void setup(){
        //have to hardcode this dependency because a DataTest which mocks out
        //the database with an in memory h2 doesn't do service scans etc to find and autowire this service!
        principalService = new PrincipalServiceImpl(repository, entityManager.getEntityManager());
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

        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        transactionTemplate.executeWithoutResult(status ->{
            Principal p1= principalService.registerIfAbsent("TEST");
            Principal p2=principalService.registerIfAbsent("test");


            assertThat(p1.id).isNotNull();
            assertThat(p1.id).isEqualTo(p2.id);
        });

    }

    @Test
    public void registerMultiple(){
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            Principal p1 = principalService.registerIfAbsent("name1");
            Principal p2 = principalService.registerIfAbsent("name2");

            assertThat(p1.id).isNotEqualTo(p2.id);
            Principal p3 = principalService.registerIfAbsent("name1");
            assertThat(p1.id).isEqualTo(p3.id);
            assertThat(p1.username).isEqualTo(p3.username);
        });
    }


}
