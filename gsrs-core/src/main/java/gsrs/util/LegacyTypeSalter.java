package gsrs.util;

import gov.nih.ncats.common.util.TimeUtil;
import lombok.Data;

import static java.lang.String.valueOf;

@Data
public class LegacyTypeSalter implements Salter {

    Hasher hasher;

    String prefix = "";

    public LegacyTypeSalter(Hasher newHasher, String newPrefix) {
        hasher = newHasher;
        prefix = newPrefix;
    }
    @Override
    public void setHasher(Hasher hasher) {
        this.hasher = hasher;
    }

    @Override
    public String generateSalt() {
        String text = "---" + TimeUtil.getCurrentDate().toString() + "---" + String.valueOf(Math.random()) + "---";
        return prefix + hasher.hash(text);
    }

    @Override
    public boolean mayBeOneOfMine(String testHash) {
        return testHash != null && testHash.startsWith(prefix);
    }
}
