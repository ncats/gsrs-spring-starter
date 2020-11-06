package ix.ginas.models.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import gsrs.repository.PrincipalRepository;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;
import java.util.Optional;

@JsonComponent
public class PrincipalDeserializer extends JsonDeserializer<Principal> {

    @Autowired
    private PrincipalRepository principalRepository;

    public PrincipalDeserializer(){

//        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);

    }

    private synchronized  void initIfNeeded(){
        if(principalRepository ==null) {
            AutowireHelper.getInstance().autowire(principalRepository);
        }
    }
    public PrincipalDeserializer(PrincipalRepository principalRepository) {
        this.principalRepository = principalRepository;
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
            Principal principal = principalRepository.findDistinctByUsernameIgnoreCase(username);

            if(principal !=null){
                return principal;
            }
            //katzelda Sept 2019 just create a new object? we can save it later on save...

            return new Principal(username, null);
        }
    }
}