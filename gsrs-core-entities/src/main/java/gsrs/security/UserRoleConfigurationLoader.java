package gsrs.security;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

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
        JsonMapper mapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        try {
            this.configuration = mapper.readValue(new File(DEFAULT_CONFIG_FILE_NAME), UserRoleConfiguration.class);
        } catch (RuntimeException ignore){

        }
    }
    public void loadConfigFromFile(String fileName) throws IOException {
        JsonMapper mapper = JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build();
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
