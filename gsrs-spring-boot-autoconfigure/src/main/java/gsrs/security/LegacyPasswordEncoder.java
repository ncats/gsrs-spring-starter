package gsrs.security;

import ix.utils.Util;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum LegacyPasswordEncoder implements PasswordEncoder {

    INSTANCE;

    private static final Pattern PATTERN = Pattern.compile("^(\\d+)\\$(\\.+)$");

    @Override
    public String encode(CharSequence rawPassword) {
        String salt = Util.generateSalt();
        return encodeRaw(rawPassword.toString(), salt);
    }

    public String encodeRaw(String rawPassword, String salt){
        String encoded = Util.encrypt(rawPassword, salt);
        return salt.length()+"$"+salt+encoded;
    }

    public String encode(String encodedPassword, String salt){
        return salt.length()+"$"+salt+encodedPassword;
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        Matcher matcher = PATTERN.matcher(encodedPassword);
        if(matcher.matches()){
            int saltLength = Integer.parseInt(matcher.group(1));
            String salt = encodedPassword.substring(0, saltLength);
            String actualEncodedPass = encodedPassword.substring(saltLength);
            String actual = Util.encrypt(rawPassword.toString(), salt);
            return actual.equals(actualEncodedPass);
        }
        return false;
    }
}
