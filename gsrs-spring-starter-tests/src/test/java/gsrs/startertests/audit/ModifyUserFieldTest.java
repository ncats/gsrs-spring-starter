package gsrs.startertests.audit;

import gov.nih.ncats.common.util.CachedSupplier;
import gsrs.AuditConfig;
import gsrs.junit.TimeTraveller;
import gsrs.model.AbstractGsrsEntity;
import gsrs.repository.PrincipalRepository;
import gsrs.services.PrincipalService;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.*;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import gsrs.startertests.jupiter.ResetAllEntityProcessorBeforeEachExtension;
import ix.core.EntityProcessor;
import ix.core.models.Principal;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@GsrsJpaTest(classes = GsrsSpringApplication.class, dirtyMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ContextConfiguration(classes = GsrsSpringApplication.class)
@ActiveProfiles("test")
public class ModifyUserFieldTest  extends AbstractGsrsJpaEntityJunit5Test {


    /**
     * I can't seem to be able to use @WithMockUser to have 2 different
     * users in the same test method. and the way junit works this withMockUser
     * is set Before the call to BeforeEach so having the audit listeners
     * set a different user on create vs update won't work.
     * So I wrote this entityProcessor which will override those fields
     * after the audit is set to change it to someone else.
     */
    private static class SetCreatedBy implements EntityProcessor<MyEntity>{

        private CachedSupplier<Void> init = CachedSupplier.runOnceInitializer(()->AutowireHelper.getInstance().autowire(this));

        @Autowired
        private PrincipalRepository principalRepository;

        /**
         * This is only called on the original create statement, updates are preUpdate() etc
         * @param obj
         * @throws FailProcessingException
         */
        @Override
        @Transactional
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

    @RegisterExtension
    public ResetAllEntityProcessorBeforeEachExtension resetAllEntityProcessorBeforeEachExtension = new ResetAllEntityProcessorBeforeEachExtension();

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TestEntityProcessorFactory entityProcessorFactory;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private PrincipalRepository principalRepository;

    @Autowired
    private PrincipalService principalService;

    @Autowired
    private AuditConfig auditConfig;

    private Long id;
    @BeforeEach
    public void addUserToRepo(){

        entityProcessorFactory.clearAll();
        entityProcessorFactory.addEntityProcessor(new SetCreatedBy());
        TransactionTemplate template = new TransactionTemplate(platformTransactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.executeWithoutResult(status-> {
                    principalService.clearCache();
                    principalRepository.deleteAll();
                    principalRepository.saveAndFlush(new Principal("myUser", null));
                    principalRepository.saveAndFlush(new Principal("otherUser", null));
                });

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
        assertThat(sut.getCreatedBy().username).isEqualToIgnoringCase("myUser");
        assertThat(sut.getLastModifiedBy().username).isEqualToIgnoringCase("otherUser");

    }

    @Test
    @WithMockUser(username = "otherUser")
    public void noAuditUpdateShouldNotUpdateLastModified(){

        MyEntity sut = entityManager.find(MyEntity.class, id);

        timeTraveller.jumpAhead(1, TimeUnit.DAYS);

        sut.setFoo("different");
        auditConfig.disableAuditingFor(()->entityManager.persistAndFlush(sut));

        assertEquals(id, sut.getId());
        assertEquals("different", sut.getFoo());
        assertThat(sut.getCreatedBy().username).isEqualToIgnoringCase("myUser");
        assertThat(sut.getLastModifiedBy().username).isEqualToIgnoringCase("myUser");

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
