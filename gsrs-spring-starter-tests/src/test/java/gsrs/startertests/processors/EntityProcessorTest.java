package gsrs.startertests.processors;

import gsrs.AuditConfig;
import gsrs.model.AbstractGsrsEntity;
import gsrs.repository.PrincipalRepository;

import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.ClearAuditorRule;
import gsrs.startertests.ClearTextIndexerRule;
import gsrs.startertests.GsrsEntityTestConfiguration;
import ix.core.EntityProcessor;

import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ContextConfiguration
@ActiveProfiles("test")
@Import({GsrsEntityTestConfiguration.class, EntityProcessorTest.MyEntityProcessor.class, ClearAuditorRule.class , ClearTextIndexerRule.class,  AuditConfig.class, AutowireHelper.class})

public class EntityProcessorTest {
    @Data
    @Entity
    public static class MyEntity extends AbstractGsrsEntity {
        @Id
        @GeneratedValue
        private Long id;

        private String foo;
    }




    private static List<String> list = new ArrayList<>();

    @BeforeEach
    public void clearList(){
        list.clear();
    }

    @Autowired
    private TestEntityManager repository;

    @Autowired
    private PrincipalRepository principalRepository;

    @Autowired
    @RegisterExtension
    ClearTextIndexerRule clearTextIndexerRule;

    @Autowired
    @RegisterExtension
    ClearAuditorRule clearAuditorRule;

    @Test
    public void prePersist(){
        MyEntity sut = new MyEntity();

        sut.setFoo("myDomain");

        repository.persistAndFlush(sut);

        assertEquals(Arrays.asList("myDomain"), list);

    }

    @Component
    public static class MyEntityProcessor implements EntityProcessor<MyEntity> {

        public MyEntityProcessor(){
            System.out.println("IN CONSTRUCTOR");
        }
        @Override
        public void prePersist(MyEntity obj) {
            list.add(obj.getFoo());
        }

        @Override
        public Class<MyEntity> getEntityClass() {
            return MyEntity.class;
        }
    }
}
