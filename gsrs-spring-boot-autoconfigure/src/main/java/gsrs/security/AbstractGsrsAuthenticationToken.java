package gsrs.security;

import ix.core.models.UserProfile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public abstract class AbstractGsrsAuthenticationToken extends UsernamePasswordAuthenticationToken {
    public AbstractGsrsAuthenticationToken(UserProfile principal, Object credentials) {
        super(principal, credentials);
    }

    public UserProfile getUserProfile(){
        return (UserProfile) getPrincipal();
    }
    @Override
    public String getName() {
        return ((UserProfile)getPrincipal()).user.username;
    }
}
