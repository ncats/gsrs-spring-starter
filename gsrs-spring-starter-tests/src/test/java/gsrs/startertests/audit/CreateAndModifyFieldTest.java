package gsrs.startertests.audit;

import gsrs.junit.TimeTraveller;
import gsrs.repository.PrincipalRepository;
import gsrs.startertests.*;
import ix.core.models.IxModel;
import ix.core.models.Principal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

import javax.persistence.Entity;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@GsrsJpaTest(dirtyMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = GsrsSpringApplication.class)
@Import({CreateAndModifyFieldTest.GsrsEntityConfig.class})

public class CreateAndModifyFieldTest {

    @Configuration
    @EntityScan(basePackages = "ix")
    public static class GsrsEntityConfig{

    }
    @RegisterExtension
    public TimeTraveller timeTraveller = new TimeTraveller(LocalDate.of(1985, 10, 21));

    @Autowired
    private TestEntityManager entityManager;


    @Autowired
    @RegisterExtension
    ClearTextIndexerRule clearTextIndexerRule;

    @Autowired
    @RegisterExtension
    ClearAuditorRule clearAuditorRule;

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

    @Test

    public void updateShouldOnlyUpdateLastModified(){
        MyEntity sut = new MyEntity();
        sut.setFoo("myFoo");
        Principal user = principalRepository.findDistinctByUsernameIgnoreCase("myUser");
        assertNotNull(user);
        sut.setCreatedBy(user);
        sut.setLastModifiedBy(user);
        entityManager.persistAndFlush(sut);

        timeTraveller.jumpAhead(1, TimeUnit.DAYS);

        sut.setFoo("different");

        entityManager.persistAndFlush(sut);

        assertEquals("different", sut.getFoo());
        assertThat(sut.getCreatedBy().username).isEqualTo("myUser");
//        assertThat(sut.getLastModifiedBy().username).isEqualTo("myUser");
    }
}
