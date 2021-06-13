package gsrs.security;

import ix.core.models.UserProfile;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

public class LegacyUserPassAuthentication extends AbstractGsrsAuthenticationToken {

    public LegacyUserPassAuthentication(UserProfile principal, Object credentials) {
        super(principal, credentials);
    }

    public String getUsername(){
        return (String) getPrincipal();
    }
}
