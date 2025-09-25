package gsrs.security;

import gsrs.services.PrivilegeService;
import ix.core.models.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
class UserProfilePasswordAuthentication implements GsrsUserProfileDetails {

    private final UserProfile up;
    private boolean authenticated =false;
    private Collection<? extends GrantedAuthority> authorities;

    private PrivilegeService privilegeService = new PrivilegeService();

    public UserProfilePasswordAuthentication(UserProfile up) {
        List<String> privileges = privilegeService.getPrivilegesForRoles(up.getRoles());
        log.trace("in authentication, privileges: {}", privileges);
        this.up = up;
        //this.authorities = up.getRoles().stream().map(r->new SimpleGrantedAuthority("ROLE_"+ r.name())).collect(Collectors.toList());
        this.authorities = privileges.stream()
                .map(p-> new SimpleGrantedAuthority(p))
                .collect(Collectors.toList());
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
