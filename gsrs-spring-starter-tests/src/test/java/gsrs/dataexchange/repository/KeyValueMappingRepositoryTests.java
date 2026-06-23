package gsrs.dataexchange.repository;

import gsrs.controller.GsrsControllerConfiguration;
import gsrs.stagingarea.model.KeyValueMapping;
import gsrs.stagingarea.repository.KeyValueMappingRepository;
import gsrs.startertests.GsrsEntityTestConfiguration;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.GsrsSpringApplication;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

@Slf4j
@ActiveProfiles("test")
@GsrsJpaTest( classes = { GsrsSpringApplication.class, GsrsControllerConfiguration.class, GsrsEntityTestConfiguration.class})
public class KeyValueMappingRepositoryTests extends AbstractGsrsJpaEntityJunit5Test {

    @Autowired
    KeyValueMappingRepository keyValueMappingRepository;

    private static final String LOCATION_ONE = "Location 1";
    private static final String LOCATION_TWO = "Location 2";

    private static UUID id1= UUID.randomUUID();
    private static UUID id2= UUID.randomUUID();

    private static boolean setupDone =false;

/*
    @BeforeEach
    public void setup() {
        System.out.println("starting in setup");
        //AutowireHelper.getInstance().autowireAndProxy( keyValueMappingRepository);
        if(!setupDone) {
            //createSomeRecords();
            setupDone=true;
        }
    }
*/

    @Test
    public void testDeleteByRecordId() {
        createSomeRecords();
        Assertions.assertEquals(4L, keyValueMappingRepository.count());
        log.trace("in testDeleteByRecordId");
        boolean before =keyValueMappingRepository.findAll().stream().anyMatch(m->m.getRecordId().equals(id1));
        keyValueMappingRepository.deleteByRecordId(id1);
        boolean after =keyValueMappingRepository.findAll().stream().anyMatch(m->m.getRecordId().equals(id1));
        Assertions.assertNotEquals(before, after);
    }

    @Test
    public void testDeleteByLocation() {
        createSomeRecords();
        log.trace("in testDeleteByLocation");
        boolean before =keyValueMappingRepository.findAll().stream().anyMatch(m->m.getDataLocation().equals(LOCATION_ONE));
        keyValueMappingRepository.deleteByDataLocation(LOCATION_ONE);
        boolean after =keyValueMappingRepository.findAll().stream().anyMatch(m->m.getDataLocation().equals(LOCATION_ONE));
        Assertions.assertNotEquals(before, after);
    }

    private void createSomeRecords() {
        KeyValueMapping mapping1 = new KeyValueMapping();
        mapping1.setDataLocation(LOCATION_ONE);
        mapping1.setRecordId(id1);
        mapping1.setKey("key 1");
        mapping1.setValue("value 1");
        keyValueMappingRepository.saveAndFlush(mapping1);

        KeyValueMapping mapping2 = new KeyValueMapping();
        mapping2.setDataLocation(LOCATION_ONE);
        mapping2.setRecordId(id1);
        mapping2.setKey("key 2");
        mapping2.setValue("value 2");
        keyValueMappingRepository.saveAndFlush(mapping2);

        KeyValueMapping mapping3 = new KeyValueMapping();
        mapping3.setDataLocation(LOCATION_TWO);
        mapping3.setRecordId(id1);
        mapping3.setKey("key 1");
        mapping3.setValue("value 1");
        keyValueMappingRepository.saveAndFlush(mapping3);

        KeyValueMapping mapping4 = new KeyValueMapping();
        mapping4.setDataLocation(LOCATION_TWO);
        mapping4.setRecordId(id1);
        mapping4.setKey("key 2");
        mapping4.setValue("value 2");
        keyValueMappingRepository.saveAndFlush(mapping4);
        log.trace("values created!");
    }
}