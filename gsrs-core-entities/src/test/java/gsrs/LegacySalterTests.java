package gsrs;

import gsrs.util.GsrsPasswordHasher;
import gsrs.util.Hasher;
import gsrs.util.LegacyTypeSalter;
import gsrs.util.Salter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LegacySalterTests {

    private final String SALT_PREFIX = "G";

    private final Hasher hasher = new GsrsPasswordHasher();

    private final Salter salter = new LegacyTypeSalter(hasher, SALT_PREFIX);

    @Test
    void testGenerateSalt() throws Exception{
        String salt1 = salter.generateSalt();
        Assertions.assertNotNull(salt1);
        System.out.printf("salt: %s\n", salt1);
    }

    @Test
    void testGenerateOwnSalt() throws Exception{
        String salt1 = salter.generateSalt();
        Assertions.assertTrue(salter.mayBeOneOfMine(salt1));
    }

    @Test
    void testGenerateNotOwnSalt() throws Exception {
        String salt1 = salter.generateSalt();
        String changedSalt = salt1.replace('G', 'N');
        Assertions.assertFalse(salter.mayBeOneOfMine(changedSalt));
    }

}
