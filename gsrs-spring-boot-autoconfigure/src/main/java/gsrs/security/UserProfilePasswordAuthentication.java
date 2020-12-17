package gsrs.security;

import ix.core.models.UserProfile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.stream.Collectors;

class UserProfilePasswordAuthentication implements GsrsUserProfileDetails {

    private final UserProfile up;
    private boolean authenticated =false;
    private Collection<? extends GrantedAuthority> authorities;
    public UserProfilePasswordAuthentication(UserProfile up) {

        this.up = up;
        this.authorities = up.getRoles().stream().map(r->new SimpleGrantedAuthority("ROLE_"+ r.name())).collect(Collectors.toList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public Object getCredentials() {
        return up.getEncodePassword();
    }

    @Override
    public Object getDetails() {
        return null;
    }

    @Override
    public UserProfile getPrincipal() {
        return up;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return up.user.username;
    }
}
