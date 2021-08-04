package gsrs.services;

import ix.core.models.Group;
import ix.core.models.Principal;

import java.util.Set;

public interface GroupService {

    Group registerIfAbsent(String name);

    void updateUsersGroups(Principal user, Set<String> newGroups);

    void clearCache();
}
