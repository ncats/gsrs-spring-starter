package ix.ginas.models.serialization;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import gsrs.services.GroupService;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.Group;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JacksonComponent;

@JacksonComponent
public class GroupDeserializer extends ValueDeserializer<Group> {
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
        }
    }
    public Group deserialize
            (JsonParser parser, DeserializationContext ctx) {
        initIfNeeded();
        String name=parser.getValueAsString();
        return groupService.registerIfAbsent(name);

    }
}


