package gsrs;

import gov.nih.ncats.common.util.Caches;
import gov.nih.ncats.common.util.TimeUtil;
import gsrs.repository.PrincipalRepository;
import gsrs.security.GsrsUserProfileDetails;
import ix.core.models.Principal;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * This is a config class that tells Spring how to do JPA auditing (like get the current user etc
 * and automatically set the created by, last edited by fields on the entities).
 *
 * This also overrides how the auditing gets the current time so we can
 * change the time inside tests.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "timeTraveller")
@Slf4j
public class AuditConfig {

    @PersistenceContext(unitName =  DefaultDataSourceConfig.NAME_ENTITY_MANAGER)
    private EntityManager entityManager;
    
    @Bean
    public AuditorAware<Principal> createAuditorProvider(PrincipalRepository principalRepository
//            , @Qualifier(DefaultDataSourceConfig.NAME_ENTITY_MANAGER)  EntityManager em
            ) {
        return new SecurityAuditor(principalRepository, entityManager);
    }
    
    @Bean
    @Primary
    public DateTimeProvider timeTraveller(){
        return ()-> {

            Optional<TemporalAccessor> dt = Optional.of(TimeUtil.getCurrentLocalDateTime());

            return dt;
        };
    }
    @Bean
    public AuditingEntityListener createAuditingListener() {
        return new AuditingEntityListener();
    }

    public static class SecurityAuditor implements AuditorAware<Principal> {
        private PrincipalRepository principalRepository;

        private EntityManager em;
        //use an LRU Cache of name look ups. without this on updates to Substances
        //we get a stackoverflow looking up the name over and over for some reason...
        //need to synchronize it in case multiple threads are adding users at the same time
        //otherwise we get concurrent modification exceptions.
        //since we only do computeIfAbsent call and clear the synchronized blocks
        //shouldn't cause too much of a performance hit
        private Map<String, Optional<Principal>> principalCache = Collections.synchronizedMap(Caches.createLRUCache(100));

        public void clearCache(){
            principalCache.clear();
        }
        public SecurityAuditor(PrincipalRepository principalRepository, EntityManager em) {
            this.principalRepository = principalRepository;
            this.em = em;
        }

        @Override
        @Transactional
        public Optional<Principal> getCurrentAuditor() {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if(auth ==null || auth instanceof AnonymousAuthenticationToken){
                return Optional.empty();
            }

            if(auth instanceof GsrsUserProfileDetails){
                //refetch from repository because the one from the authentication is "detached"
                return principalRepository.findById(((GsrsUserProfileDetails)auth).getPrincipal().user.id);

            }
            String name = auth.getName();

            if(name ==null){
                return Optional.empty();
            }
            //
            log.debug("looking up principal for " + name + " from class "+ auth.getClass());
//            System.out.println("looking up principal for " + name + " from class "+ auth.getClass());

            Optional<Principal> value = principalCache.computeIfAbsent(name,
                    n -> {
                        //if name doesn't exist it will return null which won't get entered into the map and could
                        //cause a stackoverflow of constantly re-looking up a non-existant value
                        //so we save them as Optionals
                        //TODO should we use configuration to add new user if missing?
                        try {
                            Principal p = principalRepository.findDistinctByUsernameIgnoreCase(n);
                            return Optional.ofNullable(p);
                        } catch (Throwable t) {
                            return Optional.empty();
                        }

                    });
            if(value.isPresent()){
                Principal p = value.get();
                return Optional.of(em.contains(p)? p : em.merge(p));
            }
            return value;
        }
    }	
}
