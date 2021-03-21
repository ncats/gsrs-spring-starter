package gsrs.services;


import ix.core.models.Principal;

public interface PrincipalService {

    Principal registerIfAbsent(String name);
}
