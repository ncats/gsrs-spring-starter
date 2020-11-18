package gsrs.services;


import gsrs.repository.PrincipalRepository;
import ix.core.models.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class PrincipalServiceImpl implements PrincipalService {

    private final PrincipalRepository principalRepository;

    @Autowired
    public PrincipalServiceImpl(PrincipalRepository principalRepository) {
        this.principalRepository = principalRepository;
    }

    @Override
    public Principal registerIfAbsent(Principal p){
        Principal alreadyInDb = principalRepository.findDistinctByUsernameIgnoreCase(p.username);
        if(alreadyInDb!=null){
            return alreadyInDb;
        }
        principalRepository.save(p);
        return p;
    }
}
