package gsrs.services;


import gsrs.repository.PrincipalRepository;
import ix.core.models.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Optional;

@Service
public class PrincipalServiceImpl implements PrincipalService {

    private final PrincipalRepository principalRepository;

    @Autowired
    public PrincipalServiceImpl(PrincipalRepository principalRepository) {
        this.principalRepository = principalRepository;
    }

    @Override
    public Principal registerIfAbsent(String username){
        Principal alreadyInDb = principalRepository.findDistinctByUsernameIgnoreCase(username);
        if(alreadyInDb!=null){
            return alreadyInDb;
        }
        System.out.println("creating principal " + username);
        return principalRepository.save(new Principal(username, null));
    }
}
