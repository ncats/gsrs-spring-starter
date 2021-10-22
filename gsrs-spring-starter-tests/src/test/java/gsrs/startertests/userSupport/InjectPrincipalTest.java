package gsrs.startertests.userSupport;

import gsrs.EntityPersistAdapter;
import gsrs.repository.PrincipalRepository;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.TestEntityProcessorFactory;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import gsrs.startertests.jupiter.ResetAllEntityProcessorBeforeEachExtension;
import ix.core.EntityProcessor;
import ix.core.models.Principal;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

@GsrsJpaTest
public class InjectPrincipalTest extends AbstractGsrsJpaEntityJunit5Test {

    @Autowired
    private TestEntityManager entityManager;

    private static Principal myUser, otherUser;

    @RegisterExtension
    ResetAllEntityProcessorBeforeEachExtension resetAllEntityProcessorBeforeEachExtension = new ResetAllEntityProcessorBeforeEachExtension();

    @Autowired
    private TestEntityProcessorFactory entityProcessorFactory;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;



    private static boolean initialized = false;

    @BeforeEach
    public void init(){
        if(!initialized) {
            initialized=true;
            myUser = new Principal("myUser", null);


            otherUser = new Principal("otherUser", null);
            TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            tx.executeWithoutResult(ignored -> {


                entityManager.persistAndFlush(otherUser);
                entityManager.persistAndFlush(myUser);
            });
        }
    }

    @Test
    @WithMockUser(username = "myUser")
    public void create(){
        EntityWithUser sut = new EntityWithUser();
        sut.setFoo("foo");

        EntityWithUser saved = entityManager.persistAndFlush(sut);
        assertEquals("foo", saved.getFoo());
        //because we get different transactions these might be different objects

        assertEquals(myUser.username, saved.getCreatedBy().username);
        assertEquals(myUser.username, saved.getModifiedBy().username);
        assertNotNull(saved.getId());
    }

    @Test
    @WithMockUser(username = "myUser")
    public void update(){

        entityProcessorFactory.setEntityProcessors(new EntityProcessor<EntityWithUser>(){

            @Override
            public Class<EntityWithUser> getEntityClass() {
                return EntityWithUser.class;
            }

            @Override
            public void prePersist(EntityWithUser obj) throws FailProcessingException {
                obj.setCreatedBy(otherUser);
                obj.setModifiedBy(otherUser);
            }
        });
        EntityWithUser sut = new EntityWithUser();
        sut.setFoo("foo");

        EntityWithUser saved = entityManager.persistAndFlush(sut);
        assertEquals("foo", saved.getFoo());
        assertEquals(otherUser, saved.getCreatedBy());
        assertEquals(otherUser, saved.getModifiedBy());
        assertNotNull(saved.getId());

        saved.setFoo("newFoo");
        Long id = saved.getId();
        EntityWithUser updated= entityManager.persistAndFlush(saved);
        assertEquals("newFoo", updated.getFoo());
        assertEquals(otherUser.username, updated.getCreatedBy().username);
        assertEquals(myUser.username, updated.getModifiedBy().username);
        assertEquals(id, updated.getId());
    }
}
