package gsrs;

import com.google.common.cache.CacheBuilder;
import gov.nih.ncats.common.util.Caches;
import gov.nih.ncats.common.util.TimeUtil;
import gov.nih.ncats.common.util.Unchecked;
import gsrs.repository.PrincipalRepository;
import gsrs.security.GsrsUserProfileDetails;
import gsrs.security.hasAdminRole;
import ix.core.models.Principal;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentMap;

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

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    private static ThreadLocal<Boolean> turnOffAuditing = ThreadLocal.withInitial(()-> Boolean.FALSE);

    /**
     * Temporarily turn off auditing for while
     * the given runnable is executed.
     * This will make hibernate calls to get the current datetime
     * and current user to return empty Optionals which
     * should mean hibernate won't update the last edited/created fields.
     *
     * Note that because this is done in a threadlocal way just while this
     * runnable is run any persistent changes should be flushed inside this runnable
     * otherwise it is likely the calls to get the current editor and datetime
     * won't be called until AFTER auditing is turned back on.
     * @param throwingRunnable
     * @param <E> the Throwable or Exception that the ThrowingRunnable can throw.
     * @throws E the Throwable thrown by the ThrowingRunnable.
     */
    @hasAdminRole
    public <E extends Throwable> void disableAuditingForThrowable(Unchecked.ThrowingRunnable<E> throwingRunnable) throws E{
        Objects.requireNonNull(throwingRunnable);
        turnOffAuditing.set(Boolean.TRUE);
        try{
            throwingRunnable.run();
        }finally{
            //is this enough? does a delayed DB flush get run now or could it run after?  should we document to force flush?
            turnOffAuditing.set(Boolean.FALSE);
        }
    }


    /**
     * Temporarily turn off auditing for while
     * the given runnable is executed.
     * This will make hibernate calls to get the current datetime
     * and current user to return empty Optionals which
     * should mean hibernate won't update the last edited/created fields.
     *
     * Note that because this is done in a threadlocal way just while this
     * runnable is run any persistent changes should be flushed inside this runnable
     * otherwise it is likely the calls to get the current editor and datetime
     * won't be called until AFTER auditing is turned back on.
     * @param runnable the Runnable to run without Auditing.
     */
    @hasAdminRole
    public void disableAuditingFor(Runnable runnable) {
        Objects.requireNonNull(runnable);
        turnOffAuditing.set(Boolean.TRUE);
        try{
            runnable.run();
        }finally{
            //is this enough? does a delayed DB flush get run now or could it run after?  should we document to force flush?
            turnOffAuditing.set(Boolean.FALSE);
        }
    }

    @Bean
    public AuditorAware<Principal> createAuditorProvider(PrincipalRepository principalRepository
//            , @Qualifier(DefaultDataSourceConfig.NAME_ENTITY_MANAGER)  EntityManager em
            ) {
        return new SecurityAuditor(principalRepository, entityManager,platformTransactionManager);
    }
    
    @Bean
    @Primary
    public DateTimeProvider timeTraveller(){
        return ()-> {
            if(turnOffAuditing.get().booleanValue()){
                return Optional.empty();
            }
            return Optional.of(TimeUtil.getCurrentLocalDateTime());

        };
    }
    @Bean
    public AuditingEntityListener createAuditingListener() {
        return new AuditingEntityListener();
    }

    public class SecurityAuditor implements AuditorAware<Principal> {
        private PrincipalRepository principalRepository;

        private PlatformTransactionManager transactionManager;
        private EntityManager em;
        //use an LRU Cache of name look ups. without this on updates to Substances
        //we get a stackoverflow looking up the name over and over for some reason...
        //need to synchronize it in case multiple threads are adding users at the same time
        //otherwise we get concurrent modification exceptions.
        //since we only do computeIfAbsent call and clear the synchronized blocks
        //shouldn't cause too much of a performance hit
        private ConcurrentMap<String, Optional<Principal>> principalCache = CacheBuilder.newBuilder()
                                                                                        .maximumSize(100L)
                                                                                        .<String, Optional<Principal>>build().asMap();

        public void clearCache(){
            principalCache.clear();
        }
        public SecurityAuditor(PrincipalRepository principalRepository, EntityManager em, PlatformTransactionManager platformTransactionManager) {
            this.principalRepository = principalRepository;
            this.em = em;
            this.transactionManager=platformTransactionManager;
        }

        @Override
        @Transactional
        public Optional<Principal> getCurrentAuditor() {
            if(turnOffAuditing.get().booleanValue()){
                return Optional.empty();
            }
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if(auth ==null || auth instanceof AnonymousAuthenticationToken){
                return Optional.empty();
            }

            if(auth instanceof GsrsUserProfileDetails){
                //refetch from repository because the one from the authentication is "detached"
                //TODO: does that matter?
                TransactionTemplate tx = new TransactionTemplate(transactionManager);
                tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                tx.setReadOnly(true);
                return tx.execute(s->principalRepository.findById(((GsrsUserProfileDetails)auth).getPrincipal().user.id));

            }
            String name = auth.getName();

            if(name ==null){
                return Optional.empty();
            }
            //
            log.debug("looking up principal for " + name + " from class "+ auth.getClass());
//            System.out.println("looking up principal for " + name + " from class "+ auth.getClass());
            try {
                Optional<Principal> value = principalCache.computeIfAbsent(name,
                        n -> {
                            //if name doesn't exist it will return null which won't get entered into the map and could
                            //cause a stackoverflow of constantly re-looking up a non-existant value
                            //so we save them as Optionals
                            //TODO should we use configuration to add new user if missing?
                            try {
                                TransactionTemplate tx = new TransactionTemplate(transactionManager);
                                tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
                                tx.setReadOnly(true);
                                Principal p = tx.execute(s->principalRepository.findDistinctByUsernameIgnoreCase(n));
//                                Principal p = principalRepository.findDistinctByUsernameIgnoreCase(n);
                                return Optional.ofNullable(p);
                            } catch (Throwable t) {
                                return Optional.empty();
                            }

                        });
                if (value.isPresent()) {
                    Principal p = value.get();
                    //I don't think we need to have principal attached?
                    return value;
//                return Optional.of(em.contains(p)? p : em.merge(p));
                }
                return value;
            }catch(Throwable t){
                t.printStackTrace();
                return Optional.empty();
            }
        }
    }	
}
