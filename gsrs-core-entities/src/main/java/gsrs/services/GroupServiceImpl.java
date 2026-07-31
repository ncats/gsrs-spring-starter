package gsrs.services;

import gsrs.repository.GroupRepository;
import ix.core.models.Group;
import ix.core.models.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Set;

@Service
public class GroupServiceImpl implements GroupService{

    private final GroupRepository repository;

    @Autowired
    public GroupServiceImpl(GroupRepository repository, EntityManager entityManager) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void updateUsersGroups(Principal user, Set<String> newGroups) {
        //need to clear out old groups first
        List<Group> oldGroups = repository.findGroupsByMembers(user);
        //check to see if new Groups and old Groups have in common
        for(Group g : oldGroups){
            if(!newGroups.contains(g.name)) {
                g.removeMember(user);
            }
        }
        for(String g : newGroups){
            registerIfAbsent(g).addMember(user);
        }

    }
    @Override
    public void clearCache(){
        // Retained for compatibility; entity instances are not cached.
    }
    @Override
    public Group registerIfAbsent(String name) {
        Group group = repository.findByNameIgnoreCase(name);
        if(group == null){
            group = repository.save(new Group(name));
        }
        return group;
    }
}