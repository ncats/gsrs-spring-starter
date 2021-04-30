package gsrs.startertests.processors;

import gsrs.model.AbstractGsrsEntity;
import gsrs.repository.BackupRepository;
import gsrs.startertests.GsrsEntityTestConfiguration;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.GsrsSpringApplication;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.models.Backup;
import ix.core.models.BackupEntity;
import ix.core.models.BaseModel;
import ix.core.models.LongBaseModel;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.*;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {GsrsSpringApplication.class,  GsrsEntityTestConfiguration.class})
@ActiveProfiles("test")
public class BackupProcessorTest extends AbstractGsrsJpaEntityJunit5Test {
    @Data
    @Entity
    @Backup

    public static class MyBackedUpEntity extends BaseModel {
        @Id
        private Long id;

        @Column(unique=true)
        private String foo;

        @Override
        public String fetchGlobalId() {
            return getClass().getName() +":" + foo;
        }
    }

    @Data
    @Entity
    public static class NotBackedupEntity extends LongBaseModel {


        private String foo;
    }

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private BackupRepository backupRepository;

    @Autowired
    PlatformTransactionManager platformTransactionManager;



    TransactionTemplate transactionTemplate;

    CountDownLatch latch;
    @BeforeEach
    public void setUp() throws Exception {
        transactionTemplate = new TransactionTemplate(platformTransactionManager);
        latch = new CountDownLatch(1);

    }

    @Test
    public void persistNotBackup() throws Exception {
        NotBackedupEntity sut = new NotBackedupEntity();
        sut.setFoo("bar");
        transactionTemplate.executeWithoutResult(status-> {
            entityManager.persist(sut);
            status.flush();
        });
        assertEquals(0, backupRepository.count());
    }

    @Test
    public void persistBackup() throws Exception {
        MyBackedUpEntity sut = new MyBackedUpEntity();
        sut.setFoo("bar");

        sut.setId(1234L);
        transactionTemplate.executeWithoutResult(status-> {
            entityManager.persist(sut);
            status.flush();
        });

        assertNotNull(entityManager.find(MyBackedUpEntity.class, 1234L));
        assertEquals(1, backupRepository.count());

        BackupEntity be = backupRepository.findByRefid(sut.fetchGlobalId()).get();

        MyBackedUpEntity fromJson = (MyBackedUpEntity) be.getInstantiated();
        assertEquals(sut, fromJson);
    }
}
