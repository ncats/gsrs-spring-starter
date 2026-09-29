package gsrs.startertests.audit;

import gsrs.controller.GsrsControllerConfiguration;
import gsrs.stagingarea.service.DefaultStagingAreaService;
import gsrs.startertests.GsrsEntityTestConfiguration;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.GsrsSpringApplication;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.search.text.IndexerServiceFactory;
import ix.core.search.text.TextIndexerFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@GsrsJpaTest(
    classes = {
       GsrsSpringApplication.class,
        GsrsControllerConfiguration.class,
        GsrsEntityTestConfiguration.class
    },
    dirtyMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class StagingAreaWiringTest extends AbstractGsrsJpaEntityJunit5Test {

    @Autowired
    ApplicationContext context;

    @MockitoSpyBean
    IndexerServiceFactory indexerServiceFactory;

    @MockitoSpyBean
    TextIndexerFactory textIndexerFactory;

    @Test
    void hasOnlyOneTextIndexerFactoryBean() {
        assertEquals(
                1,
                context.getBeansOfType(TextIndexerFactory.class).size()
        );
    }

    @Test
    void createsOnlyOneWriterForEachIndexDirectory() throws Exception {
        Path ixHome = canonical(tempDir.toPath());
        Path imports = canonical(ixHome.resolve("imports"));

        verify(indexerServiceFactory, times(1))
                .createForDir(argThat(file ->
                        canonical(file.toPath()).equals(ixHome)));

        verify(indexerServiceFactory, times(1))
                .createForDir(argThat(file ->
                        canonical(file.toPath()).equals(imports)));
    }

    @Test
    void stagingServiceUsesInjectedFactoryForImports() {
        clearInvocations(textIndexerFactory);

        AutowireCapableBeanFactory beanFactory =
                context.getAutowireCapableBeanFactory();

        DefaultStagingAreaService<?> service =
                beanFactory.createBean(DefaultStagingAreaService.class);

        File expected =
                new File(tempDir, "imports").getAbsoluteFile();

        verify(textIndexerFactory).getInstance(
                argThat(file ->
                        canonical(file.toPath())
                                .equals(canonical(expected.toPath())))
        );

        beanFactory.destroyBean(service);
    }

    private static Path canonical(Path path) {
        try {
            return path.toRealPath();
        } catch (IOException e) {
            return path.toAbsolutePath().normalize();
        }
    }
}
