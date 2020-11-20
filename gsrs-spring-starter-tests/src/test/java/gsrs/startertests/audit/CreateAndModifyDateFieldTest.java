package gsrs.startertests.audit;

import gsrs.junit.TimeTraveller;
import gsrs.model.AbstractGsrsEntity;
import gsrs.startertests.ClearAuditorRule;
import gsrs.startertests.ClearTextIndexerRule;
import gsrs.startertests.GsrsJpaTest;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.test.annotation.DirtiesContext;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@GsrsJpaTest(dirtyMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class CreateAndModifyDateFieldTest {
    @Entity
    @Data
    public static class MyEntity extends AbstractGsrsEntity {
        @Id
        @GeneratedValue
        private Long id;

        @CreatedDate
        private LocalDate creationDate;

        @LastModifiedDate
        private LocalDate lastModifiedDate;

        private String foo;

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

    @Test
    public void intialCreationShouldSetCreateDateAndLastModifiedToSameValue(){
        MyEntity sut = new MyEntity();
        sut.setFoo("myFoo");

        entityManager.persistAndFlush(sut);

        assertEquals("myFoo", sut.getFoo());
        assertThat(sut.getCreationDate(), is(timeTraveller.getCurrentLocalDate()) );
        assertThat(sut.getLastModifiedDate(), is(timeTraveller.getCurrentLocalDate()) );
    }

    @Test
    public void updateShouldOnlyUpdateLastModified(){
        MyEntity sut = new MyEntity();
        sut.setFoo("myFoo");

        entityManager.persistAndFlush(sut);

        timeTraveller.jumpAhead(1, TimeUnit.DAYS);

        sut.setFoo("different");

        entityManager.persistAndFlush(sut);

        assertEquals("different", sut.getFoo());
        assertThat(sut.getCreationDate(), is(timeTraveller.getWhereWeWere().get().asLocalDate()) );
        assertThat(sut.getLastModifiedDate(), is(timeTraveller.getCurrentLocalDate()) );
    }
}
