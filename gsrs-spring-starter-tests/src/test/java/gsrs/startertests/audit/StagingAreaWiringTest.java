package gsrs.startertests.audit;

import gsrs.stagingarea.service.DefaultStagingAreaService;
import gsrs.startertests.GsrsJpaTest;
import ix.core.search.text.IndexerServiceFactory;
import ix.core.search.text.TextIndexer;
import ix.core.search.text.TextIndexerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@GsrsJpaTest(dirtyMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
class StagingAreaWiringTest {

    @Autowired
    ApplicationContext context;

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("ix.home",
                () -> tempDir.resolve("ginas.ix").toString());
        }

    @MockitoSpyBean
    private IndexerServiceFactory indexerServiceFactory;
    @Test
    void hasOnlyOneTextIndexerFactoryBean() {
        assertEquals(
                1,
                context.getBeansOfType(TextIndexerFactory.class).size()
        );
    }

    @Test
    void stagingServiceUsesInjectedFactoryForImports() {
        TextIndexerFactory factory = mock(TextIndexerFactory.class);
        TextIndexer importsIndexer = mock(TextIndexer.class);

        Path ixHome = tempDir.resolve("ginas.ix");
        File importsDir = ixHome.resolve("imports").toFile();

        when(factory.getInstance(importsDir))
                .thenReturn(importsIndexer);

        DefaultStagingAreaService<?> service =
                new DefaultStagingAreaService<>();

        service.setupIndexer();

        verify(factory).getInstance(importsDir);
        verifyNoMoreInteractions(factory);
    }
}
