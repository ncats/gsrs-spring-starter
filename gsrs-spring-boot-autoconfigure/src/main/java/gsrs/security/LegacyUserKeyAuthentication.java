package gsrs.security;

import ix.core.models.UserProfile;

import java.util.List;

public class LegacyUserKeyAuthentication extends AbstractGsrsAuthenticationToken {

    public LegacyUserKeyAuthentication(UserProfile up, String credentials) {
        super(up, credentials);
    }

    public LegacyUserKeyAuthentication(UserProfile up, String credentials, List<String> privileges) {
        super(up, credentials, privileges);
    }

}
