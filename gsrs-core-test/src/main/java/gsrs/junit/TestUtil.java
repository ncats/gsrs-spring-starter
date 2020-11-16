package gsrs.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public final class TestUtil {

    public static void assertAreEqualsAndHashCodeSame(Object a, Object b){
        assertEquals(a, b);
        if(a !=null && b!=null) {
            assertEquals(a.hashCode(), b.hashCode());
        }
    }
    public static void assertAreNotEqualsAndHashCodeDifferent(Object a, Object b){
        assertNotEquals(a, b);
        if(a !=null && b!=null) {
            assertNotEquals(a.hashCode(), b.hashCode());
        }
    }
}
