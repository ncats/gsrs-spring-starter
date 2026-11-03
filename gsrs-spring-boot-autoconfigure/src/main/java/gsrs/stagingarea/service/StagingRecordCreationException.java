package gsrs.stagingarea.service;

/**
 * Indicates that a staging-area record could not be created atomically.
 *
 * <p>This is intentionally an unchecked exception so Spring marks the active
 * transaction for rollback.</p>
 */
public class StagingRecordCreationException extends RuntimeException {

    public StagingRecordCreationException(String message) {
        super(message);
    }

    public StagingRecordCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
