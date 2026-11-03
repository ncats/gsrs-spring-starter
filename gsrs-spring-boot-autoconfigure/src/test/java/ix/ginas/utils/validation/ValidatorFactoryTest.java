package ix.ginas.utils.validation;

import gsrs.springUtils.AutowireHelper;
import gsrs.validator.DefaultValidatorConfig;
import gsrs.validator.ValidatorConfig;
import ix.core.interfaces.GsrsJsonMapper;
import ix.core.validator.ValidatorCallback;
import ix.core.validator.ValidatorCategory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValidatorFactoryTest {

    @BeforeAll
    static void configureAutowireHelper() {
        StaticApplicationContext applicationContext = new StaticApplicationContext();
        applicationContext.refresh();
        AutowireHelper helper = new AutowireHelper();
        helper.setApplicationContext(applicationContext);
    }

    @Test
    void stagingConfigurationCanRetainBatchValidatorBehavior() {
        AtomicInteger stagingExcludedSupportsCalls = new AtomicInteger();
        AtomicReference<ValidatorConfig.METHOD_TYPE> includedMethodType = new AtomicReference<>();

        DefaultValidatorConfig stagingExcluded = configFor((newValue, oldValue, methodType) -> {
            stagingExcludedSupportsCalls.incrementAndGet();
            return true;
        });
        stagingExcluded.setExcludedMethodTypes(Set.of(ValidatorConfig.METHOD_TYPE.STAGING));

        DefaultValidatorConfig included = configFor((newValue, oldValue, methodType) -> {
            includedMethodType.set(methodType);
            return methodType == ValidatorConfig.METHOD_TYPE.BATCH;
        });

        ValidatorFactory factory = new ValidatorFactory(List.of(stagingExcluded, included), null);

        factory.createValidatorFor(new Object(), null,
                ValidatorFactory.ValidationContext.stagingWithBatchBehavior(),
                ValidatorCategory.CATEGORY_ALL());

        assertEquals(0, stagingExcludedSupportsCalls.get());
        assertEquals(ValidatorConfig.METHOD_TYPE.BATCH, includedMethodType.get());
    }

    private static DefaultValidatorConfig configFor(SupportsOnlyValidator plugin) {
        DefaultValidatorConfig config = new DefaultValidatorConfig() {
            @Override
            public ValidatorPlugin newValidatorPlugin(GsrsJsonMapper mapper, ClassLoader classLoader) {
                return plugin;
            }
        };
        config.setNewObjClass(Object.class);
        return config;
    }

    @FunctionalInterface
    private interface SupportsOnlyValidator extends ValidatorPlugin<Object> {
        @Override
        default void validate(Object newValue, Object oldValue, ValidatorCallback callback) {
        }
    }
}
