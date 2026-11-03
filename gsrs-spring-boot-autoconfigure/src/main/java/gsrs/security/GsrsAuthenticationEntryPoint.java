package gsrs.security;

import gsrs.controller.GsrsControllerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

/**
 * Gsrs Specific {@link AuthenticationEntryPoint}
 * so that if authentication fails it will check the GSRS Configuration
 * to override the potential return status code.
 */
public class GsrsAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Autowired
    private GsrsControllerConfiguration gsrsControllerConfiguration;

    private JsonMapper mapper = JsonMapper.builderWithJackson2Defaults().build();
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        int status = gsrsControllerConfiguration.getStatusFor(HttpServletResponse.SC_UNAUTHORIZED, request.getParameterMap());
        Object json = GsrsControllerConfiguration.createStatusJson("unauthorized", status);
        response.setStatus(status);
        response.getWriter().append(mapper.writer().writeValueAsString(json));
        response.setContentType("application/json");
    }

}
