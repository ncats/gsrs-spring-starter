package gsrs.security;

import gsrs.repository.PrincipalRepository;
import gsrs.repository.UserProfileRepository;
import ix.core.models.Principal;
import ix.core.models.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.stream.Collectors;

//@Component
public class LegacyGsrsAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private UserProfileRepository repository;

    @Autowired
    private PrincipalRepository principalRepository;

    @Autowired
    private LegacyAuthenticationConfiguration authenticationConfiguration;

    @Autowired(required = false)
    private UserTokenCache userTokenCache;
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if(authentication instanceof UserProfilePasswordAuthentication){
            return authentication;
        }
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
                return new UserProfilePasswordAuthentication(up);
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

                    return new UserProfilePasswordAuthentication(up);

                }else{
                    throw new BadCredentialsException("invalid credentials for username" + auth.getUsername());
                }

            }
            //TODO handle token and other types of authentication
        }else if(userTokenCache !=null && authentication instanceof LegacyUserTokenAuthentication){
            LegacyUserTokenAuthentication auth = (LegacyUserTokenAuthentication) authentication;
            String token = (String) auth.getCredentials();
            UserProfile up = userTokenCache.getUserProfileFromToken(token);
            if(up !=null){

                UserProfile refetched = repository.findByUser_Username(up.user.username);
                if(refetched.acceptToken(token)){
                    return new UserProfilePasswordAuthentication(refetched);
                }
            }

        }
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UserProfilePasswordAuthentication.class.isAssignableFrom(authentication)
        || LegacySsoAuthentication.class.isAssignableFrom(authentication)
                || LegacyUserPassAuthentication.class.isAssignableFrom(authentication)
                || LegacyUserTokenAuthentication.class.isAssignableFrom(authentication)
                ;
    }

}
