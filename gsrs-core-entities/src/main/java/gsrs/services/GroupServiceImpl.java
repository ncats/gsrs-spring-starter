package gsrs.services;

import gsrs.repository.GroupRepository;
import ix.core.models.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import java.util.Map;
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

    public void clearCache(){
        cache.clear();
    }
    @Override
    public Group registerIfAbsent(String name) {
        Group  group= cache.computeIfAbsent(name, n->{
            Group g = repository.findByName(n);
            if(g ==null){
                g = new Group(n);
            }
            return g;
        });
        if(!entityManager.contains(group)){
            return entityManager.merge(group);
        }
        return group;
    }
}
