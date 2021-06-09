package gsrs.security;

import gsrs.controller.GsrsControllerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Gsrs Specific {@link AuthenticationEntryPoint}
 * so that if authentication fails it will check the GSRS Configuration
 * to override the potential return status code.
 */
public class GsrsAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(gsrsControllerConfiguration.getStatusFor(HttpServletResponse.SC_UNAUTHORIZED, request.getParameterMap()));
    }

}
