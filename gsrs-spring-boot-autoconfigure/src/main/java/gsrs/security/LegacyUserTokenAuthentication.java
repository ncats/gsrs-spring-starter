package gsrs.security;

import ix.core.models.UserProfile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class LegacyUserTokenAuthentication extends AbstractGsrsAuthenticationToken {

    public LegacyUserTokenAuthentication(UserProfile up, String credentials) {
        super(up, credentials);
    }


}
