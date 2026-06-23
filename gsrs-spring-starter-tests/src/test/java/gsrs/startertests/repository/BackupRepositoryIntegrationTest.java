package gsrs.startertests.repository;


import gsrs.repository.BackupRepository;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.models.*;
import jakarta.persistence.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureTestEntityManager;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.assertEquals;

@GsrsJpaTest
@ActiveProfiles("test")
public class BackupRepositoryIntegrationTest extends AbstractGsrsJpaEntityJunit5Test {
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private BackupRepository backupRepository;

    @Test
    public void saveEntity(){
        BackupEntity be = new BackupEntity();
        be.setVersion(1L);
        backupRepository.save(be);
        assertEquals(1L, backupRepository.count());
        BackupEntity be2 = backupRepository.findById(1L).get();
        be2.setVersion(20L);
        backupRepository.save(be2);
        BackupEntity be3 = backupRepository.findById(1L).get();
        assertEquals(20L, be3.getVersion());

        BackupEntity beEm = new BackupEntity();
        beEm.setVersion(15L);
        entityManager.persist(beEm);
        Long beEmId = beEm.getId();
        BackupEntity be4 = entityManager.find(BackupEntity.class, 1L);
        assertEquals(20L, be4.getVersion());
        BackupEntity be5 = entityManager.find(BackupEntity.class, beEmId);
        assertEquals(15L, be5.getVersion());
    }


}
