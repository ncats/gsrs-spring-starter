package gsrs;

import gsrs.repository.ControlledVocabularyRepository;
import gsrs.repository.PrincipalRepository;
import gsrs.security.GsrsSecurityConfig;
import gsrs.springUtils.AutowireHelper;
import ix.core.EntityProcessor;
import ix.ginas.models.v1.ControlledVocabulary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ContextConfiguration(classes = LuceneSpringDemoApplication.class)
@ActiveProfiles("test")
@Import({ClearAuditorRule.class ,ClearTextIndexerRule.class,  AuditConfig.class, AutowireHelper.class, GsrsSecurityConfig.class})
public class EntityProcessorTest {

    private static List<String> list = new ArrayList<>();

    @BeforeEach
    public void clearList(){
        list.clear();
    }

    @Autowired
    private ControlledVocabularyRepository repository;

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
        ControlledVocabulary sut = new ControlledVocabulary();

        sut.setDomain("myDomain");

        repository.saveAndFlush(sut);

        assertEquals(Arrays.asList("myDomain"), list);

    }

    @Component
    public static class MyEntityProcessor implements EntityProcessor<ControlledVocabulary> {

        @Override
        public void prePersist(ControlledVocabulary obj) {
            list.add(obj.getDomain());
        }

        @Override
        public Class<ControlledVocabulary> getEntityClass() {
            return ControlledVocabulary.class;
        }
    }
}
