package gsrs.security;

import ix.core.models.UserProfile;

import java.util.List;

public class SessionIdAuthentication extends AbstractGsrsAuthenticationToken {

    public SessionIdAuthentication(UserProfile up, String sessionId) {
        super(up, sessionId);
    }

    public SessionIdAuthentication(UserProfile up, String sessionId, List<String> privileges) {
        super(up, sessionId, privileges);
    }
}
