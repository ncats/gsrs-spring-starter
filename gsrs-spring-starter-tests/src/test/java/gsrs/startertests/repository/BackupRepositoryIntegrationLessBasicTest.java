package gsrs.startertests.repository;

import gsrs.events.BackupEvent;
import gsrs.repository.BackupRepository;
import gsrs.services.BackupService;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import gsrs.startertests.processors.BackupProcessorTest;
import ix.core.History;
import ix.core.models.*;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@GsrsJpaTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)

public class BackupRepositoryIntegrationLessBasicTest extends AbstractGsrsJpaEntityJunit5Test {

    @Data
    @Entity
    @Backup
    @Indexable(indexed = false)
    @History(store = false)
    @EqualsAndHashCode(callSuper=false)
    public static class MyBackedUpEntity extends BaseModel {

        @Id
        @GeneratedValue(strategy = GenerationType.SEQUENCE)

        public Long id;

        @Override
        public String fetchGlobalId() {
            if(id!=null)return this.getClass().getName() + ":" + id.toString();
            return null;
        }

        private String foo;

        public void setFoo(String foo) {
            if(this.foo !=null){
                setIsDirty("foo");

            }
//            setIsDirty("foo");
            this.foo = foo;
        }

    }


    @Autowired
    BackupService backupService;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private BackupRepository backupRepository;


    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Test
    public void callBackupDirectlyTest() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        BackupEntity be1 = new BackupEntity();
        be1.setVersion(1L);
        BackupEvent bev = new BackupEvent();
        bev.setReBackupTaskId(UUID.randomUUID());
        bev.setSource(be1);
        transactionTemplate.executeWithoutResult(status -> {
            try {
                backupService.backup(bev);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        assertEquals(1L, backupRepository.count());
    }

    @Test

    public void callBackupServiceBackupIfNeededDirectlyTest() {
        // Trying to mimic behavior with service and event listening?
        BackupProcessorTest.MyBackedUpEntity myBackedUpEntity = new BackupProcessorTest.MyBackedUpEntity();

        myBackedUpEntity.setFoo("bar");
        myBackedUpEntity.setIsAllDirty();
        BackupEntity backupEntityForConsumer = new BackupEntity();
        try {
            backupEntityForConsumer.setInstantiated(myBackedUpEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
        backupEntityForConsumer.setVersion(1L);
        backupService.backupIfNeeded(myBackedUpEntity, b-> {
            BackupEvent event = BackupEvent.builder()
                    .source(backupEntityForConsumer)
                    .build();
        });
        assertEquals(1L, backupRepository.count());
    }

}


