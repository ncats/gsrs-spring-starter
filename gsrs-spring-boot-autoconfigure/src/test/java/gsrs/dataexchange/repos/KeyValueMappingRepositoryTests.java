package gsrs.dataexchange.repos;

import gsrs.holdingarea.model.KeyValueMapping;
import gsrs.holdingarea.repository.KeyValueMappingRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.UUID;

@Slf4j
@Configuration
@Import(KeyValueMappingRepository.class)
public class KeyValueMappingRepositoryTests  {

    @Autowired
    KeyValueMappingRepository keyValueMappingRepository;

    private static final String LOCATION_ONE = "Location 1";
    private static final String LOCATION_TWO = "Location 2";
    
    private static UUID id1= UUID.randomUUID();
    private static UUID id2= UUID.randomUUID();

    private static boolean setupDone =false;

    @BeforeEach
    public void setup() {
        System.out.println("starting in setup");
        //AutowireHelper.getInstance().autowireAndProxy( keyValueMappingRepository);
        if(!setupDone) {
            //createSomeRecords();
            setupDone=true;
        }
    }

    @Test
    public void testDeleteByRecordId() {
        createSomeRecords();
        log.trace("in testDeleteByRecordId");
        boolean before =keyValueMappingRepository.findAll().stream().anyMatch(m->m.getRecordId().equals(id1));
        keyValueMappingRepository.deleteByRecordId(id1);
        boolean after =keyValueMappingRepository.findAll().stream().anyMatch(m->m.getRecordId().equals(id1));
        Assertions.assertNotEquals(before, after);
    }

    @Test
    public void testDeleteByLocation() {
        log.trace("in testDeleteByLocation");
        boolean before =keyValueMappingRepository.findAll().stream().anyMatch(m->m.getDataLocation().equals(LOCATION_ONE));
        keyValueMappingRepository.deleteByDataLocation(LOCATION_ONE);
        boolean after =keyValueMappingRepository.findAll().stream().anyMatch(m->m.getDataLocation().equals(LOCATION_ONE));
        Assertions.assertNotEquals(before, after);
    }

    private void createSomeRecords() {
        KeyValueMapping mapping1 = new KeyValueMapping();
        mapping1.setDataLocation(LOCATION_ONE);
        mapping1.setMappingId(UUID.randomUUID());
        mapping1.setRecordId(id1);
        mapping1.setKey("key 1");
        mapping1.setValue("value 1");
        keyValueMappingRepository.saveAndFlush(mapping1);

        KeyValueMapping mapping2 = new KeyValueMapping();
        mapping2.setDataLocation(LOCATION_ONE);
        mapping2.setMappingId(UUID.randomUUID());
        mapping2.setRecordId(id1);
        mapping2.setKey("key 2");
        mapping2.setValue("value 2");
        keyValueMappingRepository.saveAndFlush(mapping2);

        KeyValueMapping mapping3 = new KeyValueMapping();
        mapping3.setDataLocation(LOCATION_TWO);
        mapping3.setMappingId(UUID.randomUUID());
        mapping3.setRecordId(id1);
        mapping3.setKey("key 1");
        mapping3.setValue("value 1");
        keyValueMappingRepository.saveAndFlush(mapping3);

        KeyValueMapping mapping4 = new KeyValueMapping();
        mapping4.setDataLocation(LOCATION_TWO);
        mapping4.setMappingId(UUID.randomUUID());
        mapping4.setRecordId(id1);
        mapping4.setKey("key 2");
        mapping4.setValue("value 2");
        keyValueMappingRepository.saveAndFlush(mapping4);
        log.trace("values created!");
    }
}