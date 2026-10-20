package gsrs.imports;

import org.junit.jupiter.api.Test;

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
}
