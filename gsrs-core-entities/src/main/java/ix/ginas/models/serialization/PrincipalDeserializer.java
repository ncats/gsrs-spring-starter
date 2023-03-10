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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JsonComponent;
import org.springframework.security.core.parameters.P;

import java.io.IOException;

@JsonComponent
@Slf4j
public class PrincipalDeserializer extends JsonDeserializer<Principal> {

    @Autowired
    private PrincipalService principalService;

    public PrincipalDeserializer(){

//        SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);

    }

    private synchronized  void initIfNeeded(){

        if (principalService == null) {
            try {
                AutowireHelper.getInstance().autowire(this);
                } catch(Exception ex) {
                    log.error("Failure to autowire PrincipalService " , ex);
                }
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
            if(principalService==null){
                //note: this will be a detached principal object that may lead to errors when entity is saved
                return new Principal(username,null);
            }
            try {
                return principalService.registerIfAbsent(username);
            }
            catch (Exception ex) {
                return new Principal(username,null);
            }
        }
    }
}