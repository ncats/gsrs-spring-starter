package ix.ginas.models.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import gsrs.services.GroupService;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;


@JsonComponent
public class GroupDeserializer extends JsonDeserializer<Group> {
    @Autowired
    private GroupService groupService;

    public GroupDeserializer(GroupService groupService) {
        this.groupService = groupService;
    }

    //needed for Jackson
    public GroupDeserializer() {

    }

    private synchronized void initIfNeeded(){
        if(groupService ==null){
            AutowireHelper.getInstance().autowire(this);
//            SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
//            System.out.println(groupRepository);
        }
    }
    public Group deserialize
            (JsonParser parser, DeserializationContext ctx)
            throws IOException, JsonProcessingException {
        initIfNeeded();
        String name=parser.getValueAsString();
        return groupService.registerIfAbsent(name);

    }
}


