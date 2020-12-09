package gsrs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * A Spring Controller Advice that is used to intercept different
 * Exceptions thrown by the controller so we can centralize
 * how to respond.
 *
 * This class used the {@link GsrsControllerConfiguration}
 * to allow the API to override the error code.
 */
@RestControllerAdvice(assignableTypes = AbstractGsrsEntityController.class)
public class GsrsApiControllerAdvice {
    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity handleUnchecked(RuntimeException ex, WebRequest request){
        return gsrsControllerConfiguration.handleError(ex, request);
    }

    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    public ResponseEntity<Object> handleAccessDeniedException(Exception ex, WebRequest request) {
        return gsrsControllerConfiguration.handleError(HttpStatus.UNAUTHORIZED.value(), ex, request);

    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity handleInvalidJsonInput(HttpMessageNotReadableException ex, WebRequest request){
        return gsrsControllerConfiguration.handleError(ex, request);
    }
}
