package gsrs.security;

import ix.core.models.UserProfile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.stream.Collectors;

public abstract class AbstractGsrsAuthenticationToken extends UsernamePasswordAuthenticationToken {
    public AbstractGsrsAuthenticationToken(UserProfile principal, Object credentials) {
        super(principal, credentials,
                principal.getRoles().stream().map(r->new SimpleGrantedAuthority("ROLE_"+ r.name())).collect(Collectors.toList()));
    }

    public UserProfile getUserProfile(){
        return (UserProfile) getPrincipal();
    }
    @Override
    public String getName() {

        UserProfile up = getUserProfile();
        if(up ==null){
            return null;
        }
        return up.user.username;
    }
}
