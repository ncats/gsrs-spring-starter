package gsrs.security;

import gsrs.repository.PrincipalRepository;
import gsrs.repository.UserProfileRepository;
import ix.core.models.Principal;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.stream.Collectors;

@Component
public class LegacyGsrsAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private UserProfileRepository repository;

    @Autowired
    private PrincipalRepository principalRepository;

    @Autowired
    private LegacyAuthenticationConfiguration authenticationConfiguration;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if(authentication instanceof LegacySsoAuthentication){
            LegacySsoAuthentication auth = (LegacySsoAuthentication) authentication;
            UserProfile up = repository.findByUser_Username(auth.getUsername());
            if(up ==null && authenticationConfiguration.isAutoregister()){
                    Principal p = new Principal(auth.getUsername(), auth.getEmail());
                    up = new UserProfile(p);
                    if(authenticationConfiguration.isAutoregisteractive()){
                        up.active = true;
                    }
                    up.systemAuth = false;
                    //should cascade new Principal
                    repository.saveAndFlush(up);
                }
            if(up !=null){

                return new UsernamePasswordAuthenticationToken(auth.getUsername(), auth.getEmail(),
                        up.getRoles().stream().map(r->new SimpleGrantedAuthority("ROLE_"+ r.name())).collect(Collectors.toList()));
            }
        }else if(authentication instanceof LegacyUserPassAuthentication){
            LegacyUserPassAuthentication auth = (LegacyUserPassAuthentication) authentication;
            UserProfile up = repository.findByUser_Username(auth.getUsername());
            if(up ==null && authenticationConfiguration.isAutoregister()) {
                Principal p = new Principal(auth.getUsername(), null);
                up = new UserProfile(p);
                if (authenticationConfiguration.isAutoregisteractive()) {
                    up.active = true;
                }
                up.systemAuth = false;
                //should cascade new Principal
                repository.saveAndFlush(up);

            }
            if(up!=null){
                String rawPassword = (String) auth.getCredentials();
                if(up.acceptPassword(rawPassword)){
                    //valid password!
                    return new UsernamePasswordAuthenticationToken(auth.getUsername(), up.getEncodePassword(),
                            up.getRoles().stream().map(r->new SimpleGrantedAuthority("ROLE_"+ r.name())).collect(Collectors.toList()));

                }

            }
            //TODO handle token and other types of authentication
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return LegacySsoAuthentication.class.isAssignableFrom(authentication)
                || LegacyUserPassAuthentication.class.isAssignableFrom(authentication)
                || LegacyUserTokenAuthentication.class.isAssignableFrom(authentication)
                ;
    }

    private static class UserProfilePasswordAuthentication implements Authentication{

        private final UserProfile up;
        private boolean authenticated =false;
        public UserProfilePasswordAuthentication(UserProfile up) {
            this.up = up;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return up.getRoles().stream().map(r->new SimpleGrantedAuthority(r.name())).collect(Collectors.toList());
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
        public Object getPrincipal() {
            return up;
        }

        @Override
        public boolean isAuthenticated() {
            return true;
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
    private static class UserProfileUserDetails implements UserDetails {
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
}
