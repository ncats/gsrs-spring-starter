package gsrs.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class UserRoleConfigurationLoader {

    private final UserRoleConfiguration configuration;
    private final String CONFIG_FILE_NAME = "roles_config.json";

    public UserRoleConfigurationLoader() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        this.configuration = mapper.readValue(new File(CONFIG_FILE_NAME), UserRoleConfiguration.class);
    }

    public UserRoleConfiguration getConfiguration(){
        return this.configuration;
    }
}
