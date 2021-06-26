package gsrs.security;

import ix.core.models.UserProfile;

public class SessionIdAuthentication extends AbstractGsrsAuthenticationToken {

    public SessionIdAuthentication(UserProfile up, String sessionId) {
        super(up, sessionId);
    }


}
