package gsrs.services;


import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.persistence.EntityManager;

import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import gsrs.repository.PrincipalRepository;
import ix.core.models.Principal;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PrincipalServiceImpl implements PrincipalService {

    private final PrincipalRepository principalRepository;

    private EntityManager entityManager;


    private final Map<String, Principal> cache = new ConcurrentHashMap<>();


    @Autowired
    public PrincipalServiceImpl(PrincipalRepository principalRepository, EntityManager entityManager) {
        this.principalRepository = principalRepository;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Principal> findById(Long id) {
        return principalRepository.findById(id);
    }

    @Override
    public void clearCache() {
        cache.clear();
    }

    @Override
    public Principal registerIfAbsent(String username){
        Boolean[] created = new Boolean[]{Boolean.FALSE};
        log.info("currently there are " + principalRepository.count() + " principals in db");

        Principal p= cache.computeIfAbsent(username.toUpperCase(), name-> {
            // __aw__ should we not update the cache from database?
            log.info("= currently there are " + principalRepository.count() + " principals in db");
            Principal alreadyInDb = principalRepository.findDistinctByUsernameIgnoreCase(name);
            if (alreadyInDb != null) {
                return alreadyInDb;
            }
            log.info("creating principal " + username);
            created[0] = Boolean.TRUE;
            return new Principal(username, null);
        });

        //entity might be detached
        if(TransactionSynchronizationManager.isActualTransactionActive()){
            if(created[0].booleanValue()){
                return principalRepository.saveAndFlush(p);
            }
            try {
                // We shouldn't need to merge the principals into the entity manager for this transaction
                // unless there's something that's going to be written. Trying and potentially failing
                // to merge during a read-only will often result in an unnecessary rollback,
                // on transaction completion, even if everything else is working.


                if(!TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
//                  //  if(!entityManager.contains(p)){
                    // __aw__ not contains didn't work correctly, perhaps because id was null in some cases
                    // See PrincipalRepositoryIntegrationTest.

                    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
                    CriteriaQuery<Principal> cq = cb.createQuery(Principal.class);
                    Root<Principal> principal = cq.from(Principal.class);
                    Predicate nameCriteria = cb.equal(cb.upper(principal.get("username")), username.toUpperCase());
                    cq.where(nameCriteria);
                    TypedQuery<Principal> query = entityManager.createQuery(cq);
                    List<Principal> pl = query.getResultList();
                    if (pl.isEmpty()) {
                        return entityManager.merge(p);
                    } else {
                        return  pl.get(0);
                    }
                }
            }catch(Exception e) {
                log.error("Trouble merging principal from cache, is cache for principal stale?", e);
                
                //TODO: this detach mechanism isn't proven to do anything
                // we need at this time.
                entityManager.detach(p);
//                
            }
        }
        return p;
    }
}
