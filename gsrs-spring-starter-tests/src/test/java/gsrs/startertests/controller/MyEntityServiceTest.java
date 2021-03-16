package gsrs.startertests.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.junit.TimeTraveller;
import gsrs.service.AbstractGsrsEntityService;
import gsrs.service.GsrsEntityService;
import gsrs.startertests.*;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static gsrs.assertions.GsrsMatchers.*;
import static org.hamcrest.MatcherAssert.*;
import static org.junit.jupiter.api.Assertions.*;


@ActiveProfiles("test")
@GsrsJpaTest(classes = { GsrsSpringApplication.class, GsrsControllerConfiguration.class, GsrsEntityTestConfiguration.class, MyEntityRepository.class})
@Import({MyEntity.class, MyEntityService.class})
public class MyEntityServiceTest extends AbstractGsrsJpaEntityJunit5Test {

    @Autowired
    private MyEntityService myEntityService;

    @RegisterExtension
    TimeTraveller timeTraveller = new TimeTraveller(LocalDate.of(1955, 11, 05));

    private JacksonTester<MyEntity> json;
    ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private EntityManager em;

    @BeforeEach
    public void setup() {

        JacksonTester.initFields(this, objectMapper);
    }
    @Test
    public void noDataLoadedShouldHave0Results() throws Exception {
        assertEquals(0, myEntityService.count());
    }

    @Test
    public void loadSingleRecord() throws Exception {
        MyEntity myEntity = new MyEntity();
        myEntity.setFoo("myFoo");

        AbstractGsrsEntityService.CreationResult<MyEntity> result = myEntityService.createEntity(objectMapper.valueToTree(myEntity));
        assertTrue(result.isCreated());
        MyEntity savedMyEntity = result.getCreatedEntity();

        assertNotNull(savedMyEntity.getUuid());
        assertThat(savedMyEntity, matchesExample(MyEntity.builder()
                .foo("myFoo")
                .created(timeTraveller.getWhereWeAre().asDate())
                .modified(timeTraveller.getWhereWeAre().asDate())
                .version(1)
                .build()));
    }

    @Test
    public void loadedRecordShouldHavePreviousStateSetManually() throws Exception {
        MyEntity myEntity = new MyEntity();
        myEntity.setFoo("myFoo");
       em.persist(myEntity);
        em.flush();

        MyEntity loadedEntity = em.find(MyEntity.class, myEntity.getUuid());

        em.refresh(loadedEntity);
        assertNotNull(loadedEntity.getPreviousState());
    }
    @Test
    public void loadedRecordShouldHavePreviousStateSetFromEntityService() throws Exception {
        MyEntity myEntity = new MyEntity();
        myEntity.setFoo("myFoo");
        AbstractGsrsEntityService.CreationResult<MyEntity> result = myEntityService.createEntity(objectMapper.valueToTree(myEntity));
        assertTrue(result.isCreated());
        MyEntity savedMyEntity = result.getCreatedEntity();

        MyEntity copy= objectMapper.treeToValue(objectMapper.valueToTree(savedMyEntity), MyEntity.class);
        copy.setFoo("otherValue");
       GsrsEntityService.UpdateResult<MyEntity>  updateResult = myEntityService.updateEntity(objectMapper.valueToTree(copy));

        String oldLoadedJson = updateResult.getUpdatedEntity().getPreviousState().toString();
        assertTrue(oldLoadedJson.contains("myFoo"), oldLoadedJson);
    }
    @Test

    public void callingGetManyTimesDoesNotIncrementVersion() throws Exception {
        MyEntity myEntity = new MyEntity();
        myEntity.setFoo("myFoo");

        AbstractGsrsEntityService.CreationResult<MyEntity> result = myEntityService.createEntity(objectMapper.valueToTree(myEntity));
        assertTrue(result.isCreated());
        MyEntity savedMyEntity = result.getCreatedEntity();

        assertNotNull(savedMyEntity.getUuid());
        for(int i=0 ;i< 10; i++) {
            assertThat(myEntityService.getEntityBySomeIdentifier(savedMyEntity.getUuid().toString()).get(), matchesExample(MyEntity.builder()
                    .foo("myFoo")
                    .created(timeTraveller.getWhereWeAre().asDate())
                    .modified(timeTraveller.getWhereWeAre().asDate())
                    .version(1)
                    .build()));
        }
    }

    @Test
    public void loadTwoDifferentRecords() throws Exception {
        MyEntity myEntity = new MyEntity();
        myEntity.setFoo("myFoo");

        AbstractGsrsEntityService.CreationResult<MyEntity> result = myEntityService.createEntity(objectMapper.valueToTree(myEntity));
        assertTrue(result.isCreated());
        MyEntity savedMyEntity = result.getCreatedEntity();
        assertEquals("myFoo", savedMyEntity.getFoo());
        assertNotNull(savedMyEntity.getUuid());


        assertThat(savedMyEntity, matchesExample(MyEntity.builder()
                                    .foo("myFoo")
                                    .created(timeTraveller.getWhereWeAre().asDate())
                                    .modified(timeTraveller.getWhereWeAre().asDate())
                                    .version(1)
                                    .build()));


        MyEntity myEntity2 = new MyEntity();
        myEntity2.setFoo("myFoo2");

        AbstractGsrsEntityService.CreationResult<MyEntity> result2 = myEntityService.createEntity(objectMapper.valueToTree(myEntity2));
        assertTrue(result2.isCreated());
        MyEntity savedMyEntity2 = result2.getCreatedEntity();
        assertNotNull(savedMyEntity2.getUuid());


        assertThat(savedMyEntity2, matchesExample(MyEntity.builder()
                .foo("myFoo2")
                .created(timeTraveller.getWhereWeAre().asDate())
                .modified(timeTraveller.getWhereWeAre().asDate())
                .version(1)
                .build()));

    }

    @Test
    public void updateSingleRecord() throws Exception {
        MyEntity myEntity = new MyEntity();
        myEntity.setFoo("myFoo");

        AbstractGsrsEntityService.CreationResult<MyEntity> result = myEntityService.createEntity(objectMapper.valueToTree(myEntity));
        assertTrue(result.isCreated());
        MyEntity savedMyEntity = result.getCreatedEntity();

        UUID uuid = savedMyEntity.getUuid();
        assertNotNull(uuid);
        assertThat(savedMyEntity, matchesExample(MyEntity.builder()
                .foo("myFoo")
                .created(timeTraveller.getWhereWeAre().asDate())
                .modified(timeTraveller.getWhereWeAre().asDate())
                .version(1)
                .build()));

        MyEntity copy =  objectMapper.convertValue(objectMapper.valueToTree(savedMyEntity), MyEntity.class);
        copy.setFoo("updatedFoo");
        timeTraveller.jumpAhead(1, TimeUnit.DAYS);

        AbstractGsrsEntityService.UpdateResult<MyEntity> updateResult = myEntityService.updateEntity(objectMapper.valueToTree(copy));

        assertEquals(AbstractGsrsEntityService.UpdateResult.STATUS.UPDATED, updateResult.getStatus());
        assertThat(updateResult.getUpdatedEntity(), matchesExample(MyEntity.builder()
                .foo("updatedFoo")
                .uuid(uuid)
                .created(timeTraveller.getWhereWeWere().get().asDate())
                .modified(timeTraveller.getWhereWeAre().asDate())
                .version(2)
                .build()));
    }


}
