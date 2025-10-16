package gsrs.security;

import gsrs.services.PrivilegeService;
import ix.core.models.UserProfile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public abstract class AbstractGsrsAuthenticationToken extends UsernamePasswordAuthenticationToken {
    PrivilegeService privilegeService = PrivilegeService.instance();

    public AbstractGsrsAuthenticationToken(UserProfile principal, Object credentials) {

        super(principal, credentials,
                
                Optional.ofNullable(principal)
                .orElse(null)
                .getRoles()
                .stream()

                .map(r->new SimpleGrantedAuthority("ROLE_"+ r.getRole()))
                .collect(Collectors.toList())
                
                );
    }

    public AbstractGsrsAuthenticationToken(UserProfile principal, Object credentials, List<String> authorityNames)  {

        super(principal, credentials, authorityNames.stream()
                .map(an-> new SimpleGrantedAuthority(an))
                .collect(Collectors.toList()));
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
