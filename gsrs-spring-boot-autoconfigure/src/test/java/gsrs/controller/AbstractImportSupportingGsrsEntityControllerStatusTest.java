package gsrs.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbstractImportSupportingGsrsEntityControllerStatusTest {

    @Test
    void resolvesExplicitActionErrorStatus() {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("httpStatus", HttpStatus.INTERNAL_SERVER_ERROR.value());

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                AbstractImportSupportingGsrsEntityController.resolveActionHttpStatus(result));
    }

    @Test
    void missingActionStatusFailsClosed() {
        ObjectNode result = JsonNodeFactory.instance.objectNode();

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                AbstractImportSupportingGsrsEntityController.resolveActionHttpStatus(result));
    }

    @Test
    void failedAsyncJobReturnsAnHttpError() {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("jobStatus", "failed");
        result.put("error", true);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR,
                AbstractImportSupportingGsrsEntityController.resolveProcessingJobHttpStatus(result));
    }

    @Test
    void runningAsyncJobCanStillBePolled() {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("jobStatus", "processing");

        assertEquals(HttpStatus.OK,
                AbstractImportSupportingGsrsEntityController.resolveProcessingJobHttpStatus(result));
    }
}
