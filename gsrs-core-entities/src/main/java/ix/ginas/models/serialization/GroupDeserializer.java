package ix.ginas.models.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import gsrs.repository.GroupRepository;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;


@JsonComponent
public class GroupDeserializer extends JsonDeserializer<Group> {
    @Autowired
    private GroupRepository groupRepository;

    public GroupDeserializer(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
    }

    public GroupDeserializer() {
//        initIfNeeded();

    }

    private synchronized void initIfNeeded(){
        if(groupRepository==null){
            AutowireHelper.getInstance().autowire(this);
//            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
            System.out.println(groupRepository);
        }
    }
    public Group deserialize
            (JsonParser parser, DeserializationContext ctx)
            throws IOException, JsonProcessingException {
        initIfNeeded();
        String name=parser.getValueAsString();
        Group existingGroup = groupRepository.findByNameIgnoreCase(name);
        if(existingGroup !=null){
            return existingGroup;
        }
        //FIXME katzelda Sept 2019 make this a bean I guess...
        //katzelda Oct 2019 to be consistent with PrinicpalDeserializer we don't save if absent
        //this should be done later if the substance with this group is actually saved...

//    	Group grp = AdminFactory.registerGroupIfAbsent(new Group(name));
//        return grp;
        return new Group(name);
    }
}


