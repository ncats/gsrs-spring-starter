package gsrs.security;

import gsrs.model.UserProfileAuthenticationResult;
import gsrs.repository.SessionRepository;
import gsrs.repository.UserProfileRepository;
import ix.core.models.Principal;
import ix.core.models.Session;
import ix.core.models.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.UUID;

//@Component
@Slf4j
public class LegacyGsrsAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private UserProfileRepository repository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private LegacyAuthenticationConfiguration authenticationConfiguration;

    @Autowired(required = false)
    private UserTokenCache userTokenCache;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Override
    @Transactional
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        if(authentication instanceof SessionIdAuthentication){
            SessionIdAuthentication sessionAuth= (SessionIdAuthentication)authentication;
            Session session = sessionRepository.findById(UUID.fromString((String)sessionAuth.getCredentials())).orElse(null);
            if(session !=null && !session.expired){

                return new UserProfilePasswordAuthentication(session.profile);
            }
        }
        if(authentication instanceof UserProfilePasswordAuthentication){
            return authentication;
        }
        if(authentication instanceof LegacySsoAuthentication){
            LegacySsoAuthentication auth = (LegacySsoAuthentication) authentication;
            UserProfile up =  Optional.ofNullable(repository.findByUser_UsernameIgnoreCase(auth.getUsername()))
                    .map(oo->oo.standardize())
                    .orElse(null);
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
            UserProfile up = Optional.ofNullable(repository.findByUser_UsernameIgnoreCase(auth.getUsername()))
                    .map(oo->oo.standardize())
                    .orElse(null);
                    
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
                UserProfileAuthenticationResult authenticationResult =up.acceptPassword(rawPassword);
                if(authenticationResult.matchesRepository()   ){
                    //valid password!
                    UserProfile finalUp = up;
                    if( authenticationResult.needsSave()) {
                        log.trace("going to save up within transaction");
                        TransactionTemplate transactionTemplate = new TransactionTemplate(platformTransactionManager);
                        transactionTemplate.executeWithoutResult(u->repository.saveAndFlush(finalUp));
                    }
                    return new UserProfilePasswordAuthentication(up);
                }else{
                    throw new BadCredentialsException("invalid credentials for username" + auth.getUsername());
                }

            }

        }else if(authentication instanceof LegacyUserKeyAuthentication) {
            LegacyUserKeyAuthentication auth = (LegacyUserKeyAuthentication) authentication;
            String key = (String) auth.getCredentials();
            if(key !=null){

                UserProfile refetched = Optional.ofNullable(repository.findByUser_UsernameIgnoreCase(auth.getName()))
                        .map(oo->oo.standardize())
                        .orElse(null);
                if(refetched !=null && refetched.acceptKey(key)){
                    return new UserProfilePasswordAuthentication(refetched);
                }
            }
        }else if(userTokenCache !=null && authentication instanceof LegacyUserTokenAuthentication){
            LegacyUserTokenAuthentication auth = (LegacyUserTokenAuthentication) authentication;
            String token = (String) auth.getCredentials();
            UserProfile up = userTokenCache.getUserProfileFromToken(token);
            if(up !=null){

                UserProfile refetched = Optional.ofNullable(repository.findByUser_UsernameIgnoreCase(up.user.username))
                        .map(oo->oo.standardize())
                        .orElse(null);
                if(refetched !=null && refetched.acceptToken(token)){
                    return new UserProfilePasswordAuthentication(refetched);
                }
            }

        }
        //if we get here we don't have a valid login
        if(!authenticationConfiguration.isAllownonauthenticated()){
            throw new NonAuthenticatedUserAllowedException("non-authenticated users not allowed");
        }
        return null;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UserProfilePasswordAuthentication.class.isAssignableFrom(authentication)
        || SessionIdAuthentication.class.isAssignableFrom(authentication)
        || LegacySsoAuthentication.class.isAssignableFrom(authentication)
                || LegacyUserPassAuthentication.class.isAssignableFrom(authentication)
                || LegacyUserTokenAuthentication.class.isAssignableFrom(authentication)
                ;
    }

}
