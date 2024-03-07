package gsrs;

import gsrs.util.GsrsPasswordHasher;
import gsrs.util.Hasher;
import gsrs.util.LegacyTypeSalter;
import gsrs.util.Salter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class LegacySalterTests {

    private Hasher hasher = new GsrsPasswordHasher();
    private Salter salter = new LegacyTypeSalter(hasher);
    @Test
    void testGenerateSalt() {
        String salt1 = salter.generateSalt();
        Assertions.assertNotNull(salt1);
    }

    @Test
    void testGenerateOwnSalt() {
        String salt1 = salter.generateSalt();
        Assertions.assertTrue(salter.mayBeOneOfMine(salt1));
    }

    @Test
    void testGenerateNotOwnSalt() {
        String salt1 = salter.generateSalt();
        String changedSalt = salt1.replace('G', 'N');
        Assertions.assertFalse(salter.mayBeOneOfMine(changedSalt));
    }

}
