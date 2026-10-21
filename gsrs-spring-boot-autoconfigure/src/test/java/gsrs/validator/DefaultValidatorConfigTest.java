package gsrs.validator;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultValidatorConfigTest {

    @Test
    void excludedMethodTypeDoesNotAffectOtherValidationOperations() {
        DefaultValidatorConfig config = new DefaultValidatorConfig();
        config.setNewObjClass(Object.class);
        config.setExcludedMethodTypes(Set.of(ValidatorConfig.METHOD_TYPE.STAGING));

        assertFalse(config.meetsFilterCriteria(
                new Object(), ValidatorConfig.METHOD_TYPE.STAGING));
        assertTrue(config.meetsFilterCriteria(
                new Object(), ValidatorConfig.METHOD_TYPE.CREATE));
        assertTrue(config.meetsFilterCriteria(
                new Object(), ValidatorConfig.METHOD_TYPE.UPDATE));
        assertTrue(config.meetsFilterCriteria(
                new Object(), ValidatorConfig.METHOD_TYPE.BATCH));
    }

    @Test
    void readsSpringBoundIndexedMapForExcludedMethodTypes() {
        DefaultValidatorConfig config = new DefaultValidatorConfig();
        config.setExcludedMethodTypes(Map.of("0", "STAGING"));

        assertEquals(Set.of(ValidatorConfig.METHOD_TYPE.STAGING),
                config.getExcludedMethodTypes());
    }
}
