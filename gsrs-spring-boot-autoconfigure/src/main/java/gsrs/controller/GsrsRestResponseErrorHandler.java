package gsrs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.lang.reflect.InvocationTargetException;

@ControllerAdvice
public class GsrsRestResponseErrorHandler extends ResponseEntityExceptionHandler {

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @ExceptionHandler(RuntimeException.class)
    protected ResponseEntity<Object> handleConflict(RuntimeException ex, WebRequest request) {
        GsrsControllerConfiguration.ErrorInfo info = gsrsControllerConfiguration.createErrorStatusBody(ex,500, request);
        return handleExceptionInternal(ex, info.getBody(),
                new HttpHeaders(),info.getStatus(), request);
    }
}

