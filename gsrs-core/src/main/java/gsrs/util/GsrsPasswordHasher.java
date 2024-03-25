package gsrs.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

@Slf4j
public class GsrsPasswordHasher implements Hasher {

    String preferredHashAlgorithm = "PBKDF2";
    static int iterations = 1000;
    static String characterSet ="utf8";

    private final static String HASHING_ALGORITHM = "PBKDF2WithHmacSHA512";

    @Override
    public String getHashType() {
        return this.preferredHashAlgorithm;
    }

    @Override
    public String hash(String... values) {
        if (values == null) {
            return null;
        }
        try {
            if(preferredHashAlgorithm.equals("PBKDF2")) {
                return hash(values[0], values.length > 1 ? values[1] : null, iterations);
            }
            MessageDigest md = MessageDigest.getInstance(preferredHashAlgorithm);
            for (String v : values) {
                md.update(v.getBytes(characterSet));
            }
            return toHex(md.digest());
        } catch (Exception ex) {
            log.error("Can't generate hash!", ex);
            throw new RuntimeException(ex);
        }
    }

    public static String toHex(byte[] d) {
        StringBuilder sb = new StringBuilder();
        for (byte b : d) {
            sb.append(String.format("%1$02x", b & 0xff));
        }
        return sb.toString();
    }

    public static String hash(String input, String salt, int iterations) throws NoSuchAlgorithmException, InvalidKeySpecException, UnsupportedEncodingException {
        PBEKeySpec spec = new PBEKeySpec(input.toCharArray(), salt != null ? salt.getBytes(characterSet) :
                input.getBytes(characterSet), iterations, 64 * 8);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(HASHING_ALGORITHM);

        byte[] hash = skf.generateSecret(spec).getEncoded();
        return toHex(hash);
    }
}
