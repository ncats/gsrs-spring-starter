package gsrs.services;

import gsrs.repository.GroupRepository;
import ix.core.models.Group;
import ix.core.models.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GroupServiceImpl implements GroupService{

    private final GroupRepository repository;

    private final EntityManager entityManager;

    private final Map<String, Group> cache = new ConcurrentHashMap<>();

    @Autowired
    public GroupServiceImpl(GroupRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
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
        cache.clear();
    }
    @Override
    public Group registerIfAbsent(String name) {
        boolean created[] = new boolean[]{false};
        Group  group= cache.computeIfAbsent(name, n->{
            Group g = repository.findByNameIgnoreCase(n);
            if(g ==null){
                g = new Group(n);
                created[0] = true;
            }
            return g;
        });
        if(TransactionSynchronizationManager.isActualTransactionActive()){
            if(created[0]){
                return repository.save(group);
            }
            if(!entityManager.contains(group)){
                return entityManager.merge(group);
            }
        }
        return group;
    }
}
