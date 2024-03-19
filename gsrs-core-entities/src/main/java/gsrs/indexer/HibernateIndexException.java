package gsrs.indexer;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class HibernateIndexException extends RuntimeException {

    public HibernateIndexException() {
        super();
    }

    public HibernateIndexException(
            String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public HibernateIndexException(String message, Throwable cause) {
        super(message, cause);
    }

    public HibernateIndexException(String message) {
        super(message);
    }

    public HibernateIndexException(Throwable cause) {
        super(cause);
    }
}
