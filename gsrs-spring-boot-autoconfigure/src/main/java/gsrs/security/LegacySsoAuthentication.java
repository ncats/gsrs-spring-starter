package gsrs.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

public class LegacySsoAuthentication extends UsernamePasswordAuthenticationToken {

    public LegacySsoAuthentication(String username, Object email) {
        super(username, email);
    }

    public String getUsername(){
        return (String) getPrincipal();
    }
    public String getEmail(){
        return (String) getCredentials();
    }
}
