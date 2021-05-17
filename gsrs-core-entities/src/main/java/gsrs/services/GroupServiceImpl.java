package gsrs.services;

import gsrs.repository.GroupRepository;
import ix.core.models.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GroupServiceImpl implements GroupService{

    private final GroupRepository repository;

    private final Map<String, Group> cache = new ConcurrentHashMap<>();

    @Autowired
    public GroupServiceImpl(GroupRepository repository) {
        this.repository = repository;
    }

    @Override
    public Group registerIfAbsent(String name) {
        return cache.computeIfAbsent(name.toUpperCase(), n->{
            Group g = repository.findByNameIgnoreCase(n);
            if(g ==null){
                g = new Group(n);
            }
            return g;
        });
    }
}
