package gsrs.startertests.audit;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.EntityProcessorFactory;
import gsrs.junit.TimeTraveller;
import gsrs.model.AbstractGsrsEntity;
import gsrs.repository.PrincipalRepository;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.*;
import ix.core.EntityProcessor;
import ix.core.models.Principal;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@GsrsJpaTest(dirtyMode = DirtiesContext.ClassMode.BEFORE_CLASS)
@ContextConfiguration(classes = GsrsSpringApplication.class)
@Import(ModifyUserFieldTest.MyConfig.class)
public class ModifyUserFieldTest  extends AbstractGsrsJpaEntityJunit5Test {

    @Configuration
    public static class MyConfig {
        @Bean
        public EntityProcessorFactory entityProcessorFactory() {

            return new TestEntityProcessorFactory(new SetCreatedBy());
        }
    }

    /**
     * I can't seem to be able to use @WithMockUser to have 2 different
     * users in the same test method. and the way junit works this withMockUser
     * is set Before the call to BeforeEach so having the audit listeners
     * set a different user on create vs update won't work.
     * So I wrote this entityProcessor which will override those fields
     * after the audit is set to change it to someone else.
     */
    private static class SetCreatedBy implements EntityProcessor<MyEntity>{

        private CachedSupplier<Void> init = CachedSupplier.runOnce(()->{
            AutowireHelper.getInstance().autowire(this);
            return null;
        });
        @Autowired
        private PrincipalRepository principalRepository;

        /**
         * This is only called on the original create statement, updates are preUpdate() etc
         * @param obj
         * @throws FailProcessingException
         */
        @Override
        public void prePersist(MyEntity obj) throws FailProcessingException {
            init.getSync();
            Principal myUser = principalRepository.findDistinctByUsernameIgnoreCase("myUser");
            obj.setCreatedBy(myUser);
            obj.setLastModifiedBy(myUser);
            System.out.println(obj);
        }

        @Override
        public Class<MyEntity> getEntityClass() {
            return MyEntity.class;
        }
    }
    @RegisterExtension
    public TimeTraveller timeTraveller = new TimeTraveller(LocalDate.of(1985, 10, 21));

    @Autowired
    private TestEntityManager entityManager;


    @Autowired
    private PrincipalRepository principalRepository;

    private Long id;
    @BeforeEach
    public void addUserToRepo(){
        principalRepository.save(new Principal("myUser", null));
        principalRepository.save(new Principal("otherUser", null));

        MyEntity sut = new MyEntity();
        sut.setFoo("myFoo");
        entityManager.persistAndFlush(sut);

        id = sut.getId();
    }

    @Test
    @WithMockUser(username = "otherUser")
    public void updateShouldOnlyUpdateLastModified(){

        MyEntity sut = entityManager.find(MyEntity.class, id);

        timeTraveller.jumpAhead(1, TimeUnit.DAYS);

        sut.setFoo("different");

        entityManager.persistAndFlush(sut);
        assertEquals(id, sut.getId());
        assertEquals("different", sut.getFoo());
        assertThat(sut.getCreatedBy().username).isEqualTo("myUser");
        assertThat(sut.getLastModifiedBy().username).isEqualTo("otherUser");

    }

    @Entity
    @Data
    public static class MyEntity extends AbstractGsrsEntity {
        @Id
        @GeneratedValue
        private Long id;

        @OneToOne
        @CreatedBy
        private Principal createdBy;
        @OneToOne
        @LastModifiedBy
        private Principal lastModifiedBy;

        private String foo;

    }


}