package gsrs.startertests.repository;


import gsrs.repository.BackupRepository;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.models.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import static org.junit.jupiter.api.Assertions.assertEquals;

@GsrsJpaTest
@ActiveProfiles("test")

public class BackupRepositoryIntegrationBasicTests extends AbstractGsrsJpaEntityJunit5Test {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private BackupRepository backupRepository;


    @Autowired
    private PlatformTransactionManager platformTransactionManager;

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
