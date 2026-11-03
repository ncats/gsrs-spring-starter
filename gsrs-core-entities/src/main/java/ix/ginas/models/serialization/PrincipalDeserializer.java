package ix.ginas.models.serialization;

import gsrs.services.PrincipalService;
import gsrs.springUtils.AutowireHelper;
import ix.core.models.Principal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jackson.JacksonComponent;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ValueDeserializer;

@JacksonComponent
@Slf4j
public class PrincipalDeserializer extends ValueDeserializer<Principal> {

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
    public Principal deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws JacksonException {
        initIfNeeded();
        JsonToken token = jsonParser.currentToken();
        if (JsonToken.START_OBJECT == token) {
            JsonNode tree = deserializationContext.readTree(jsonParser);
            /* this is really inconsistent with below in that we don't
             * register this principal if it's not already in the
             * persistence store..
             */
            return deserializationContext.readTreeAsValue(tree, Principal.class);
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
