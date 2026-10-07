package gsrs.cv;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.test.context.ActiveProfiles;

import gsrs.EnableGsrsApi;
import gsrs.EnableGsrsJpaEntities;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.ginas.models.v1.ControlledVocabulary;
import ix.ginas.models.v1.VocabularyTerm;

@GsrsJpaTest(classes = ControlledVocabularySequenceGeneratorTest.TestApplication.class)
@ActiveProfiles("test")
class ControlledVocabularySequenceGeneratorTest extends AbstractGsrsJpaEntityJunit5Test {

    @EnableConfigurationProperties
    @EnableGsrsApi(indexerType = EnableGsrsApi.IndexerType.LEGACY,
            entityProcessorDetector = EnableGsrsApi.EntityProcessorDetector.CUSTOM,
            indexValueMakerDetector = EnableGsrsApi.IndexValueMakerDetector.CUSTOM)
    @EnableGsrsJpaEntities
    @SpringBootApplication
    static class TestApplication {
    }

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void bootsWithControlledVocabularySequenceGenerators() {
        assertThat(entityManager.getMetamodel().entity(ControlledVocabulary.class)).isNotNull();
        assertThat(entityManager.getMetamodel().entity(VocabularyTerm.class)).isNotNull();
    }
}