package gsrs.startertests.repository;


import gsrs.repository.BackupRepository;
import gsrs.repository.PrincipalRepository;
import gsrs.services.PrincipalService;
import gsrs.services.PrincipalServiceImpl;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.models.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@GsrsJpaTest
@ActiveProfiles("test")

public class BackupRepositoryIntegration2Test extends AbstractGsrsJpaEntityJunit5Test {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private BackupRepository backupRepository;


    @Autowired
    private PlatformTransactionManager platformTransactionManager;

//    private PrincipalService principalService;

    @BeforeEach
    public void setup(){
        //have to hardcode this dependency because a DataTest which mocks out
        //the database with an in memory h2 doesn't do service scans etc to find and autowire this service!
        // principalService = new PrincipalServiceImpl(repository, entityManager.getEntityManager());
    }


    @Test
    public void persistTest1(){
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        transactionTemplate.executeWithoutResult(status ->{
            BackupEntity be1 = new BackupEntity();
            be1.setVersion(1L);
            entityManager.persist(be1);
            BackupEntity be2 = new BackupEntity();
            be2.setVersion(2L);
            entityManager.persist(be2);
        });
        assertEquals(2, backupRepository.count());
    }



}
