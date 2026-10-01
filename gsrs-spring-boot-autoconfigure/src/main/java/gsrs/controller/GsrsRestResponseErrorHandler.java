package gsrs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

//@ControllerAdvice
public class GsrsRestResponseErrorHandler implements AccessDeniedHandler {

    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    @Autowired
    @Qualifier("legacyJsonMapper")
    private JsonMapper mapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) throws ServletException {
        int status= gsrsControllerConfiguration.getStatusFor(HttpStatus.FORBIDDEN.value(), request.getParameterMap());
        response.setStatus(status);
        try {
            mapper.writeValue(response.getOutputStream(), gsrsControllerConfiguration.getError(ex,status));
        } catch (Exception e) {
            throw new ServletException();
        }
    }

}

