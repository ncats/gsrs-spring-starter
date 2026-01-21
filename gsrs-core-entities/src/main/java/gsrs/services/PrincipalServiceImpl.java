package gsrs.services;


import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
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
        Principal p= cache.computeIfAbsent(username.toUpperCase(), name-> {
            log.debug("currently there are " + principalRepository.count() + " principals in db");
            Principal alreadyInDb = principalRepository.findDistinctByUsernameIgnoreCase(name);
            if (alreadyInDb != null) {
                return alreadyInDb;
            }
            log.debug("creating principal " + username);
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
                    if(!entityManager.contains(p)){
                        return entityManager.merge(p);
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
