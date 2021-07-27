package gsrs.springUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

import javax.annotation.PostConstruct;

/**
 * This class is used to get a Bean
 * in the Spring {@link ApplicationContext}
 * even in a static context.
 *
 * This implementation was taken from
 * <a href="https://stackoverflow.com/a/17660150">https://stackoverflow.com/a/17660150</a>
 */
@Component
public class StaticContextAccessor {

    private static StaticContextAccessor instance;

    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void registerInstance() {
        instance = this;
    }

    public static <T> T getBean(Class<T> clazz) {
        if (instance != null && instance.applicationContext != null) {
            return instance.applicationContext.getBean(clazz);
        }
        return null;
    }

    public static <T> Optional<T> getOptionalBean(Class<T> clazz) {
        return Optional.ofNullable(getBean(clazz));
        
    }
}
