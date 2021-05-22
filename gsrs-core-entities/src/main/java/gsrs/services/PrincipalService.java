package gsrs.services;


import ix.core.models.Principal;

import java.util.Optional;

public interface PrincipalService {

    Principal registerIfAbsent(String name);

    Optional<Principal> findById(Long id);

    void clearCache();
}
