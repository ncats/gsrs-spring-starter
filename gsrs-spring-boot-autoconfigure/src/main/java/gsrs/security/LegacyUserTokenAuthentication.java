package gsrs.security;

import ix.core.models.UserProfile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

public class LegacyUserTokenAuthentication extends AbstractGsrsAuthenticationToken {

    public LegacyUserTokenAuthentication(UserProfile up, String credentials) {
        super(up, credentials);
    }

    public LegacyUserTokenAuthentication(UserProfile up, String credentials, List<String> privileges) {
        super(up, credentials, privileges);
    }
}
