package gsrs.services;

import ix.core.models.Group;

public interface GroupService {

    Group registerIfAbsent(String name);
}
