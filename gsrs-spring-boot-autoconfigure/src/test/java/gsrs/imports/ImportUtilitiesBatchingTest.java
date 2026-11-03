package gsrs.imports;

import gsrs.service.GsrsEntityService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportUtilitiesBatchingTest {

    @Test
    void recognizesExactChunkBoundaries() {
        assertFalse(ImportUtilities.isChunkBoundary(0, 50));
        assertFalse(ImportUtilities.isChunkBoundary(49, 50));
        assertTrue(ImportUtilities.isChunkBoundary(50, 50));
        assertFalse(ImportUtilities.isChunkBoundary(51, 50));
        assertTrue(ImportUtilities.isChunkBoundary(100, 50));
        assertFalse(ImportUtilities.isChunkBoundary(1, 0));
    }

    @Test
    void progressUpdatesOnlyAfterTheConfiguredCommittedInterval() {
        assertFalse(ImportUtilities.shouldUpdateProgress(0, 0, 100));
        assertFalse(ImportUtilities.shouldUpdateProgress(50, 0, 100));
        assertTrue(ImportUtilities.shouldUpdateProgress(100, 0, 100));
        assertFalse(ImportUtilities.shouldUpdateProgress(150, 100, 100));
        assertTrue(ImportUtilities.shouldUpdateProgress(200, 100, 100));
        assertFalse(ImportUtilities.shouldUpdateProgress(200, 0, 0));
    }

    @Test
    void saveFailureReturnsAnHttpErrorWithThePersistenceMessage() {
        GsrsEntityService.ProcessResult<Object> failedSave = GsrsEntityService.ProcessResult.builder()
                .saved(false)
                .throwable(new IllegalStateException("duplicate lower-level id"))
                .build();

        ObjectNode response = ImportUtilities.createSaveFailureResponse("staging-record-id", failedSave);

        assertEquals("staging-record-id", response.get("stagingAreaId").asText());
        assertEquals("Object failed to save", response.get("message").asText());
        assertEquals("duplicate lower-level id", response.get("error").asText());
        assertEquals("INTERNAL_SERVER_ERROR", response.get("status").asText());
        assertEquals(500, response.get("httpStatus").asInt());
    }

    @Test
    void asyncJobFailsWhenAnyRecordFails() {
        ArrayNode results = JsonNodeFactory.instance.arrayNode();
        results.add(ImportUtilities.setActionStatus(JsonNodeFactory.instance.objectNode(),
                org.springframework.http.HttpStatus.OK));
        results.add(ImportUtilities.setActionStatus(JsonNodeFactory.instance.objectNode(),
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

        assertTrue(ImportUtilities.hasFailedResults(results));
    }

    @Test
    void asyncJobCompletesWhenEveryRecordSucceeds() {
        ArrayNode results = JsonNodeFactory.instance.arrayNode();
        results.add(ImportUtilities.setActionStatus(JsonNodeFactory.instance.objectNode(),
                org.springframework.http.HttpStatus.OK));

        assertFalse(ImportUtilities.hasFailedResults(results));
    }
}
