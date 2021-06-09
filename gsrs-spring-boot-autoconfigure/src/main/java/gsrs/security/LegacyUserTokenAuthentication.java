package gsrs.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class LegacyUserTokenAuthentication extends UsernamePasswordAuthenticationToken {

    public LegacyUserTokenAuthentication( Object credentials) {
        super(null, credentials);
    }
}
