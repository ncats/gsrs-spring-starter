package gsrs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;

@RestControllerAdvice(assignableTypes = AbstractGsrsEntityController.class)
public class GsrsApiControllerAdvice {
    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity handleUnchecked(RuntimeException ex, WebRequest request){
        return gsrsControllerConfiguration.handleError(ex, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity handleInvalidJsonInput(HttpMessageNotReadableException ex, WebRequest request){
        return gsrsControllerConfiguration.handleError(ex, request);
    }
}
