package gsrs.startertests.audit;

import gsrs.AuditConfig;
import gsrs.startertests.GsrsSpringApplication;
import gsrs.model.AbstractGsrsEntity;
import gsrs.repository.PrincipalRepository;
import gsrs.services.PrincipalService;
import gsrs.startertests.*;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.models.Principal;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
@ActiveProfiles("test")
@GsrsJpaTest(dirtyMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = GsrsSpringApplication.class)

public class CreateUserFieldTest  extends AbstractGsrsJpaEntityJunit5Test {


    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PrincipalRepository principalRepository;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    private PrincipalService principalService;

    @Autowired
    private AuditConfig auditConfig;

    @BeforeEach
    public void addUserToRepo(){
        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transactionTemplate.executeWithoutResult(ignored ->{
            principalRepository.deleteAll();
            principalService.clearCache();
            principalService.registerIfAbsent("myUser");
            principalService.registerIfAbsent("otherUser");
        });

    }

    @Test
    @WithMockUser(username = "myUser")
    public void intialCreationShouldSetCreateDateAndLastModifiedToSameValue(){
        MyEntity sut = new MyEntity();
        sut.setFoo("myFoo");

        entityManager.persistAndFlush(sut);

        assertEquals("myFoo", sut.getFoo());
        assertThat(sut.getCreatedBy().username).isEqualToIgnoringCase("myUser");
        assertThat(sut.getLastModifiedBy().username).isEqualToIgnoringCase("myUser");
    }

    @Test
    @WithMockUser(username = "myUser")
    public void noAuditIntialCreationShouldNotSetCreateDateOrLastModified(){
        MyEntity sut = new MyEntity();
        sut.setFoo("myFoo");
        auditConfig.disableAuditingFor(()->entityManager.persistAndFlush(sut));


        assertEquals("myFoo", sut.getFoo());
        assertNull(sut.getCreatedBy());
        assertNull(sut.getLastModifiedBy());
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
