package gsrs.security;

import ix.core.models.UserProfile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public interface GsrsUserProfileDetails extends Authentication {
    @Override
    Collection<? extends GrantedAuthority> getAuthorities();

    @Override
    Object getCredentials();

    @Override
    Object getDetails();

    @Override
    UserProfile getPrincipal();

    @Override
    boolean isAuthenticated();

    @Override
    String getName();
}
