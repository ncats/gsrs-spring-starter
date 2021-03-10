package gsrs.services;


import ix.core.models.Principal;

public interface PrincipalService {
    Principal registerIfAbsent(Principal p);
    default Principal registerIfAbsent(String name){
        return registerIfAbsent(new Principal(name, null));
    }
}
