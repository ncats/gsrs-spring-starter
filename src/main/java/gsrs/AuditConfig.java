package gsrs;

import gov.nih.ncats.common.util.Caches;
import gov.nih.ncats.common.util.TimeUtil;
import gsrs.repository.PrincipalRepository;

import ix.core.models.Principal;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAccessor;
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
public class AuditConfig {
    @Bean
    public AuditorAware<Principal> createAuditorProvider(PrincipalRepository principalRepository) {
        return new SecurityAuditor(principalRepository);
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

        //use an LRU Cache of name look ups. without this on updates to Substances
        //we get a stackoverflow looking up the name over and over for some reason...
        private Map<String, Principal> principalCache = Caches.createLRUCache();

        public void clearCache(){
            principalCache.clear();
        }
        public SecurityAuditor(PrincipalRepository principalRepository) {
            this.principalRepository = principalRepository;
        }

        @Override
        public Optional<Principal> getCurrentAuditor() {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if(auth ==null || auth instanceof AnonymousAuthenticationToken){
                return Optional.empty();
            }
            String name = auth.getName();
            System.out.println("looking up principal for " + name);

            return Optional.ofNullable(principalCache.computeIfAbsent(name, n->principalRepository.findDistinctByUsernameIgnoreCase(name)) );
        }
    }	
}