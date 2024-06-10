package gsrs.util;

import gov.nih.ncats.common.util.TimeUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

@Data
@Slf4j
public class LegacyTypeSalter implements Salter {

    Hasher hasher;

    String prefix = "";

    public LegacyTypeSalter(Hasher newHasher, String newPrefix) {
        hasher = newHasher;
        prefix = newPrefix;
    }

    private String algorithm = "DRBG";

    @Override
    public void setHasher(Hasher hasher) {
        this.hasher = hasher;
    }

    @Override
    public String generateSalt() throws Exception {
        try {

            String text = "---" + TimeUtil.getCurrentDate().toString() + "---"
                    + String.valueOf(SecureRandom.getInstance(algorithm).nextDouble()) + "---";
            return prefix + hasher.hash(text);
        } catch (NoSuchAlgorithmException e) {
            log.error("Configured algorithm not available");
            throw e;
        }
    }

    @Override
    public boolean mayBeOneOfMine(String testHash) {
        return testHash != null && testHash.startsWith(prefix);
    }
}
