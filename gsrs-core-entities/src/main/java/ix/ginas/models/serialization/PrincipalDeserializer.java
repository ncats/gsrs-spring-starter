package ix.ginas.models.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import gsrs.services.PrincipalService;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;

@JsonComponent
public class PrincipalDeserializer extends JsonDeserializer<Principal> {

    @Autowired
    private PrincipalService principalService;

    public PrincipalDeserializer(){

//        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);

    }

    private synchronized  void initIfNeeded(){
        if(principalService ==null) {
            AutowireHelper.getInstance().autowire(this);
        }
    }
    public PrincipalDeserializer(PrincipalService principalRepository) {
        this.principalService = principalRepository;
    }

    @Override
    public Principal deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        initIfNeeded();
        JsonToken token = jsonParser.getCurrentToken();
        if (JsonToken.START_OBJECT == token) {
            JsonNode tree = jsonParser.getCodec().readTree(jsonParser);
            /* this is really inconsistent with below in that we don't
             * register this principal if it's not already in the
             * persistence store..
             */
            return jsonParser.getCodec().treeToValue(tree, Principal.class);
        }
        else { // JsonToken.VALUE_STRING:
            String username = jsonParser.getValueAsString();
            return principalService.registerIfAbsent(username);
        }
    }
}