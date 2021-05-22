package gsrs.services;


import gsrs.repository.PrincipalRepository;
import ix.core.models.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PrincipalServiceImpl implements PrincipalService {

    private final PrincipalRepository principalRepository;

    private final Map<String, Principal> cache = new ConcurrentHashMap<>();

    @Autowired
    public PrincipalServiceImpl(PrincipalRepository principalRepository) {
        this.principalRepository = principalRepository;
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
        return cache.computeIfAbsent(username.toUpperCase(), name-> {
            System.out.println("currently there are " + principalRepository.count() + " principals in db");
            Principal alreadyInDb = principalRepository.findDistinctByUsernameIgnoreCase(name);
            if (alreadyInDb != null) {
                return alreadyInDb;
            }
            System.out.println("creating principal " + username);
            return principalRepository.saveAndFlush(new Principal(username, null));
        });
    }
}
