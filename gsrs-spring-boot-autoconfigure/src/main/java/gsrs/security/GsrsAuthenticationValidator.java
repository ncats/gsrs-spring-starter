package gsrs.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("gsrsAuthenticationValidator")
public class GsrsAuthenticationValidator {

    private static final String AUTHENTICATED_REQUEST_ATTRIBUTE =
            GsrsAuthenticationValidator.class.getName() + ".authenticated";

    @Autowired
    private HttpServletRequest request;

    public void markAuthenticated() {
        request.setAttribute(AUTHENTICATED_REQUEST_ATTRIBUTE, Boolean.TRUE);
    }

    public boolean isCurrentlyAuthenticated() {
        return Boolean.TRUE.equals(request.getAttribute(AUTHENTICATED_REQUEST_ATTRIBUTE));
    }
}
