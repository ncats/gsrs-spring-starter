package gsrs.services;


import gsrs.repository.PrincipalRepository;
import ix.core.models.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
        Principal p= cache.computeIfAbsent(username.toUpperCase(), name-> {
            System.out.println("currently there are " + principalRepository.count() + " principals in db");
            Principal alreadyInDb = principalRepository.findDistinctByUsernameIgnoreCase(name);
            if (alreadyInDb != null) {
                return alreadyInDb;
            }
            System.out.println("creating principal " + username);
            return principalRepository.saveAndFlush(new Principal(username, null));
        });
        //entity might be detached
        if(!entityManager.contains(p)){
            return entityManager.merge(p);
        }
        return p;
    }
}
