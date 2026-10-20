package gsrs.stagingarea.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import gsrs.events.ReindexEntityEvent;
import gsrs.stagingarea.model.ImportMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DefaultStagingAreaServiceIndexingTest {

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publishesIndexEventOnlyAfterCommit() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        DefaultStagingAreaService<?> service = serviceWithPublisher(publisher);
        ImportMetadata metadata = metadata();
        TransactionSynchronizationManager.initSynchronization();

        service.indexAfterCommit(metadata);

        verify(publisher, never()).publishEvent(any(Object.class));
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);
        ArgumentCaptor<ReindexEntityEvent> eventCaptor =
                ArgumentCaptor.forClass(ReindexEntityEvent.class);
        verify(publisher).publishEvent(eventCaptor.capture());
        assertFalse(eventCaptor.getValue().isRequiresDelete(),
                "new staging records should be added without a redundant index delete");
    }

    @Test
    void indexingFailureAfterCommitDoesNotEscapeAndTriggerDatabaseRetry() {
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        doThrow(new IllegalStateException("index unavailable"))
                .when(publisher).publishEvent(any(Object.class));
        DefaultStagingAreaService<?> service = serviceWithPublisher(publisher);
        Logger serviceLogger = (Logger) LoggerFactory.getLogger(DefaultStagingAreaService.class);
        Level previousLevel = serviceLogger.getLevel();

        try {
            serviceLogger.setLevel(Level.OFF);
            assertDoesNotThrow(() -> service.indexAfterCommit(metadata()));
        } finally {
            serviceLogger.setLevel(previousLevel);
        }
    }

    private static DefaultStagingAreaService<?> serviceWithPublisher(
            ApplicationEventPublisher publisher) {
        DefaultStagingAreaService<?> service = new DefaultStagingAreaService<>();
        ReflectionTestUtils.setField(service, "applicationEventPublisher", publisher);
        return service;
    }

    private static ImportMetadata metadata() {
        ImportMetadata metadata = new ImportMetadata();
        metadata.setRecordId(UUID.randomUUID());
        metadata.setInstanceId(UUID.randomUUID());
        return metadata;
    }
}
