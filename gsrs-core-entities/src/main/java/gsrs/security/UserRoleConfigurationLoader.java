package gsrs.security;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

@Slf4j
public class UserRoleConfigurationLoader {

    private UserRoleConfiguration configuration;
    private final String DEFAULT_CONFIG_FILE_NAME = "roles_config.json";

    public UserRoleConfigurationLoader() {
    }

    private void loadFromDefault() {
        log.info("loaded config from default file");
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try {
            this.configuration = mapper.readValue(new File(DEFAULT_CONFIG_FILE_NAME), UserRoleConfiguration.class);
        } catch (IOException ignore){

        }
    }
    public void loadConfigFromFile(String fileName) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.configuration = mapper.readValue(new File(fileName), UserRoleConfiguration.class);
        log.info("loaded configuration from {}", fileName);
    }

    public UserRoleConfiguration getConfiguration(){
        if(this.configuration == null || this.configuration.getRoles()== null || this.configuration.getRoles().size() ==0) {
            loadFromDefault();
        }
        return this.configuration;
    }
}
