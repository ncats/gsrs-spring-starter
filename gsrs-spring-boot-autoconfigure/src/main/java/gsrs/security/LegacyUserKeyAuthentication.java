package gsrs.security;

import ix.core.models.UserProfile;

public class LegacyUserKeyAuthentication extends AbstractGsrsAuthenticationToken {

    public LegacyUserKeyAuthentication(UserProfile up, String credentials) {
        super(up, credentials);
    }


}
