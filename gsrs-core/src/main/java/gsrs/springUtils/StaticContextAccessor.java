package gsrs.springUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * This class is used to get a Bean
 * in the Spring {@link ApplicationContext}
 * even in a static context.
 *
 * This implementation was taken from
 * <a href="https://stackoverflow.com/a/17660150">https://stackoverflow.com/a/17660150</a>
 */
@Slf4j
@Component
public class StaticContextAccessor {

    private static StaticContextAccessor instance;

    
    private List<Runnable> doFinally= new ArrayList<>();
    
    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void registerInstance() {
        instance = this;
    }
    
    public static void addStaticShutdownRunnable(Runnable r) {
        if(instance!=null) {
            instance.addShutdownRunnable(r);
        }else {
            throw new IllegalStateException("no instance of " + StaticContextAccessor.class.getSimpleName() + " is accessible");
        }
    }
    
    public void addShutdownRunnable(Runnable r) {
        this.doFinally.add(r);
    }
    
    @PreDestroy
    public void testDestroy() {
//        System.out.println("REMOVE");
        doFinally.forEach(r->{
            try {
                r.run();
            }catch(Exception e) {
                log.warn("Problem running closing runnable",e);
            }
        });
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
