package gsrs.security;


import gsrs.GsrsFactoryConfiguration;
import gsrs.repository.GroupRepository;
import gsrs.repository.PrincipalRepository;
import ix.core.models.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class GsrsUserDetailsService implements UserDetailsService {

    private PrincipalRepository principalRepository;

    private GroupRepository groupRepository;
    @Autowired
    private GsrsFactoryConfiguration gsrsFactoryConfiguration;


    public GsrsUserDetailsService(PrincipalRepository principalRepository) {
        this.principalRepository = principalRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Principal principal= principalRepository.findDistinctByUsernameIgnoreCase(username);
        if(principal !=null){
            return new PrincipalUserDetails(principal);

        }
        throw new UsernameNotFoundException(username);
    }

    private static class PrincipalUserDetails implements UserDetails{
        private Principal p;

        public PrincipalUserDetails(Principal p) {
            this.p = p;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return null;
        }

        @Override
        public String getPassword() {
            return "";
        }

        @Override
        public String getUsername() {
            return p.username;
        }

        @Override
        public boolean isAccountNonExpired() {
            return false;
        }

        @Override
        public boolean isAccountNonLocked() {
            return false;
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
