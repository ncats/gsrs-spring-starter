package gsrs.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

public class LegacyUserPassAuthentication extends UsernamePasswordAuthenticationToken {

    public LegacyUserPassAuthentication(Object principal, Object credentials) {
        super(principal, credentials);
    }

    public String getUsername(){
        return (String) getPrincipal();
    }
}
