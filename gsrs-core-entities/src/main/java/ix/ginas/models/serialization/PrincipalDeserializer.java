package ix.ginas.models.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import gsrs.repository.PrincipalRepository;
import gsrs.services.PrincipalService;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@JsonComponent
public class PrincipalDeserializer extends JsonDeserializer<Principal> {

    @Autowired
    private PrincipalService principalRepository;

    public PrincipalDeserializer(){

//        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);

    }

    private synchronized  void initIfNeeded(){
        if(principalRepository ==null) {
            AutowireHelper.getInstance().autowire(this);
        }
    }
    public PrincipalDeserializer(PrincipalService principalRepository) {
        this.principalRepository = principalRepository;
    }

    @Override
    @Transactional(readOnly = true)
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
            return principalRepository.registerIfAbsent(username);
        }
    }
}