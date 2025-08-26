package gsrs.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

//@ControllerAdvice
public class GsrsRestResponseErrorHandler implements AccessDeniedHandler {

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws IOException, ServletException {
        int status= gsrsControllerConfiguration.getStatusFor(HttpStatus.FORBIDDEN.value(), request.getParameterMap());
        response.setStatus(status);
        try {
            ObjectMapper mapper = new ObjectMapper();

            mapper.writeValue(response.getOutputStream(), gsrsControllerConfiguration.getError(ex,status));
        } catch (Exception e) {
            throw new ServletException();
        }
    }

}

