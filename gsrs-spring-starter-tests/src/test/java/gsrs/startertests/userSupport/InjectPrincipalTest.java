package gsrs.startertests.userSupport;

import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.TestEntityProcessorFactory;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import gsrs.startertests.jupiter.ResetAllEntityProcessorBeforeEachExtension;
import ix.core.EntityProcessor;
import ix.core.models.Principal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.test.context.support.WithMockUser;

import static org.junit.jupiter.api.Assertions.*;

@GsrsJpaTest
public class InjectPrincipalTest extends AbstractGsrsJpaEntityJunit5Test {

    @Autowired
    private TestEntityManager entityManager;

    Principal myUser, otherUser;

    @RegisterExtension
    ResetAllEntityProcessorBeforeEachExtension resetAllEntityProcessorBeforeEachExtension = new ResetAllEntityProcessorBeforeEachExtension();

    @Autowired
    private TestEntityProcessorFactory entityProcessorFactory;

    @BeforeEach
    public void init(){
        myUser = new Principal("myUser", null);
        entityManager.persistAndFlush(myUser);

        otherUser = new Principal("otherUser", null);
        entityManager.persistAndFlush(otherUser);
    }

    @Test
    @WithMockUser(username = "myUser")
    public void create(){
        EntityWithUser sut = new EntityWithUser();
        sut.setFoo("foo");

        EntityWithUser saved = entityManager.persistAndFlush(sut);
        assertEquals("foo", saved.getFoo());
        assertEquals(myUser, saved.getCreatedBy());
        assertEquals(myUser, saved.getModifiedBy());
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
        assertEquals(otherUser, updated.getCreatedBy());
        assertEquals(myUser, updated.getModifiedBy());
        assertEquals(id, updated.getId());
    }
}
