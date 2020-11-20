package gsrs.startertests.audit;

import gsrs.model.AbstractGsrsEntity;
import gsrs.repository.PrincipalRepository;
import gsrs.startertests.*;
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
import org.springframework.test.context.ContextConfiguration;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@GsrsJpaTest(dirtyMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = GsrsSpringApplication.class)

public class CreateUserFieldTest  extends AbstractGsrsJpaEntityJunit5Test {


    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PrincipalRepository principalRepository;

    @BeforeEach
    public void addUserToRepo(){
        principalRepository.save(new Principal("myUser", null));
        principalRepository.save(new Principal("otherUser", null));
    }

    @Test
    @WithMockUser(username = "myUser")
    public void intialCreationShouldSetCreateDateAndLastModifiedToSameValue(){
        MyEntity sut = new MyEntity();
        sut.setFoo("myFoo");

        entityManager.persistAndFlush(sut);

        assertEquals("myFoo", sut.getFoo());
        assertThat(sut.getCreatedBy().username).isEqualTo("myUser");
        assertThat(sut.getLastModifiedBy().username).isEqualTo("myUser");
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
