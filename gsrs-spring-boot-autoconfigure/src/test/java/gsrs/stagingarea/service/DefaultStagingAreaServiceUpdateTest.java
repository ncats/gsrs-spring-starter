package gsrs.stagingarea.service;

import gsrs.stagingarea.model.ImportData;
import gsrs.stagingarea.model.ImportMetadata;
import gsrs.stagingarea.repository.ImportDataRepository;
import gsrs.stagingarea.repository.ImportMetadataRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class DefaultStagingAreaServiceUpdateTest {

    @Test
    void flushesNewRevisionBeforePropagationCanClearPersistenceContext() throws Exception {
        UUID recordId = UUID.randomUUID();
        ImportData existing = ImportData.builder()
                .recordId(recordId)
                .instanceId(UUID.randomUUID())
                .version(1)
                .data("old")
                .entityClassName(Object.class.getName())
                .build();
        ImportMetadata metadata = new ImportMetadata();
        metadata.setRecordId(recordId);
        metadata.setInstanceId(existing.getInstanceId());
        metadata.setEntityClassName(Object.class.getName());

        ImportDataRepository dataRepository = mock(ImportDataRepository.class);
        ImportMetadataRepository metadataRepository = mock(ImportMetadataRepository.class);
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        JsonMapper jsonMapper = mock(JsonMapper.class);
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        DefaultStagingAreaService<?> service = spy(new DefaultStagingAreaService<>());

        ReflectionTestUtils.setField(service, "importDataRepository", dataRepository);
        ReflectionTestUtils.setField(service, "metadataRepository", metadataRepository);
        ReflectionTestUtils.setField(service, "transactionManager", transactionManager);
        ReflectionTestUtils.setField(service, "jsonMapper", jsonMapper);
        ReflectionTestUtils.setField(service, "applicationEventPublisher", publisher);

        doReturn(new Object()).when(service)
                .deserializeObject(Object.class.getName(), "updated");
        doNothing().when(service).propagateUpdate(
                eq(metadata), eq("updated"), eq(Object.class.getName()), any(UUID.class));
        doReturn("clean-updated").when(jsonMapper).writeValueAsString(any());
        doReturn(List.of(existing)).when(dataRepository).retrieveDataForRecord(recordId);
        doReturn(metadata).when(metadataRepository).retrieveByRecordID(recordId);
        doReturn(mock(TransactionStatus.class)).when(transactionManager)
                .getTransaction(any(TransactionDefinition.class));

        service.updateRecord(recordId.toString(), "updated");

        ArgumentCaptor<ImportData> savedRevision = ArgumentCaptor.forClass(ImportData.class);
        verify(dataRepository).saveAndFlush(savedRevision.capture());
        assertEquals(2, savedRevision.getValue().getVersion());
        assertEquals("clean-updated", savedRevision.getValue().getData());
    }
}
