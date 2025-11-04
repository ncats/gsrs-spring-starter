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

    public UserRoleConfigurationLoader() throws IOException {
        log.info("loaded config from default file");
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.configuration = mapper.readValue(new File(DEFAULT_CONFIG_FILE_NAME), UserRoleConfiguration.class);
    }

    public void loadConfigFromFile(String fileName) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.configuration = mapper.readValue(new File(fileName), UserRoleConfiguration.class);
    }

    public UserRoleConfiguration getConfiguration(){
        return this.configuration;
    }
}
