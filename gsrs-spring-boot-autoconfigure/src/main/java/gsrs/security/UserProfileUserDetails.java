package gsrs.security;

import ix.core.models.UserProfile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class UserProfileUserDetails implements UserDetails {
    private UserProfile p;

    public UserProfileUserDetails(UserProfile p) {
        this.p = p;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return p.getEncodePassword();
    }

    @Override
    public String getUsername() {
        return p.user.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return !p.deprecated;
    }

    @Override
    public boolean isAccountNonLocked() {
        return p.active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return !p.isDeprecated();
    }
}
