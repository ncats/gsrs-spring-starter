package gsrs.startertests.processors;

import gsrs.model.AbstractGsrsEntity;
import gsrs.repository.BackupRepository;
import gsrs.startertests.GsrsEntityTestConfiguration;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.GsrsSpringApplication;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.History;
import ix.core.models.*;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.*;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
//This has to be a full spring boot Test not just a JPA test because we need the full applicaton context for all the application events to get fired and recieved
@SpringBootTest(classes = {GsrsSpringApplication.class,  GsrsEntityTestConfiguration.class})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class BackupProcessorTest extends AbstractGsrsJpaEntityJunit5Test {
    @Data
    @Entity
    @Backup
    @Indexable(indexed = false)
    @History(store = false)
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

    @Data
    @Entity
    @Indexable(indexed = false)
    @History(store = false)
    public static class NotBackedupEntity extends LongBaseModel {


        private String foo;

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
            setIsDirty("foo");
        }
    }

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private BackupRepository backupRepository;

    @Autowired
    PlatformTransactionManager platformTransactionManager;



    TransactionTemplate transactionTemplate;

    @BeforeEach
    public void setUp() throws Exception {
        transactionTemplate = new TransactionTemplate(platformTransactionManager);

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
    public void updateNotBackup() throws Exception {
        NotBackedupEntity sut = new NotBackedupEntity();
        sut.setFoo("bar");
        transactionTemplate.executeWithoutResult(status-> {
            entityManager.persist(sut);
            status.flush();
        });

        NotBackedupEntity saved2 = transactionTemplate.execute(status-> {
            NotBackedupEntity sut2 = entityManager.find(NotBackedupEntity.class, sut.id);
            sut2.setFoo("bar2");
            entityManager.persist(sut2);
            status.flush();
            return sut2;
        });
        transactionTemplate.executeWithoutResult(status-> {
            assertEquals(0, backupRepository.count());
            assertEquals("bar2", entityManager.find(NotBackedupEntity.class, saved2.id).foo);
        });
    }

    @Test
    public void persistBackup() throws Exception {
        MyBackedUpEntity sut = new MyBackedUpEntity();
        sut.setFoo("bar");

        transactionTemplate.executeWithoutResult(status-> {
                    entityManager.persist(sut);
                    entityManager.flush();
                }
        );
        assertNotNull(sut.id);
        assertNotNull(entityManager.find(MyBackedUpEntity.class, sut.id));
        assertEquals(1, backupRepository.count());

        BackupEntity be = backupRepository.findByRefid(sut.fetchGlobalId()).get();

        MyBackedUpEntity fromJson = (MyBackedUpEntity) be.getInstantiated();
        assertEquals(sut, fromJson);
    }

    @Test
    public void updateBackup() throws Exception {
        MyBackedUpEntity sut = new MyBackedUpEntity();
        sut.setFoo("bar");
        transactionTemplate.executeWithoutResult(status-> {
            entityManager.persist(sut);

            entityManager.flush();
        });

        MyBackedUpEntity saved2 = transactionTemplate.execute(status-> {
            MyBackedUpEntity sut2 = entityManager.find(MyBackedUpEntity.class, sut.id);
            sut2.setFoo("bar2");
            entityManager.persist(sut2);

            entityManager.flush();
            return sut2;
        });
        assertEquals(1, backupRepository.count());
        assertEquals("bar2", entityManager.find(MyBackedUpEntity.class, saved2.id).foo);

        BackupEntity be2 = backupRepository.findByRefid(saved2.fetchGlobalId()).get();
        MyBackedUpEntity fromJson2 = (MyBackedUpEntity) be2.getInstantiated();
        assertEquals(saved2, fromJson2);

    }


    @Test
    public void twoDifferentPersistBackupEntitiesSameTransaction() throws Exception {
        MyBackedUpEntity sut = new MyBackedUpEntity();
        sut.setFoo("bar");

        MyBackedUpEntity sut2 = new MyBackedUpEntity();
        sut2.setFoo("foobar");
        transactionTemplate.executeWithoutResult(status-> {
                    entityManager.persist(sut);
                    entityManager.persist(sut2);
                }
        );
        assertNotNull(sut.id);
        assertNotNull(entityManager.find(MyBackedUpEntity.class, sut.id));
        assertEquals(2, backupRepository.count());

        BackupEntity be = backupRepository.findByRefid(sut.fetchGlobalId()).get();

        MyBackedUpEntity fromJson = (MyBackedUpEntity) be.getInstantiated();
        assertEquals(sut, fromJson);

        BackupEntity be2 = backupRepository.findByRefid(sut2.fetchGlobalId()).get();
        MyBackedUpEntity fromJson2 = (MyBackedUpEntity) be2.getInstantiated();
        assertEquals(sut2, fromJson2);
        assertNotEquals(sut, sut2);
    }

    @Test
    public void twoDifferentPersistBackupEntitiesDiffTransaction() throws Exception {
        MyBackedUpEntity sut = new MyBackedUpEntity();
        sut.setFoo("bar");

        MyBackedUpEntity sut2 = new MyBackedUpEntity();
        sut2.setFoo("foobar");
        transactionTemplate.executeWithoutResult(status-> {
                    entityManager.persist(sut);
                }
        );

        transactionTemplate.executeWithoutResult(status-> {
                    entityManager.persist(sut2);
                }
        );
        assertNotNull(sut.id);
        assertNotNull(entityManager.find(MyBackedUpEntity.class, sut.id));
        assertEquals(2, backupRepository.count());

        BackupEntity be = backupRepository.findByRefid(sut.fetchGlobalId()).get();

        MyBackedUpEntity fromJson = (MyBackedUpEntity) be.getInstantiated();
        assertEquals(sut, fromJson);

        BackupEntity be2 = backupRepository.findByRefid(sut2.fetchGlobalId()).get();
        MyBackedUpEntity fromJson2 = (MyBackedUpEntity) be2.getInstantiated();
        assertEquals(sut2, fromJson2);
        assertNotEquals(sut, sut2);
    }
}
