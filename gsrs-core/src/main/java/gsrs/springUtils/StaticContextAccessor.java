package gsrs.springUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.persistence.EntityManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.BeanFactoryAnnotationUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import gsrs.DataSourceConfigRegistry;
import ix.core.util.EntityUtils.Key;
import lombok.extern.slf4j.Slf4j;

/**
 * This class is used to get a Bean
 * in the Spring {@link ApplicationContext}
 * even in a static context.
 * 
 * It is also used as way of accessing other global-scope
 * resources from a static context. The use of this class
 * is discouraged as there is usually a way to have a Bean
 * autowired or access the global-scope resource in a more
 * orthodox way.
 *
 * This implementation was taken from
 * <a href="https://stackoverflow.com/a/17660150">https://stackoverflow.com/a/17660150</a>
 */
@Slf4j
@Component
public class StaticContextAccessor {
    

    private static StaticContextAccessor instance;

    // This is a hack to have a global-last ditch
    // place for things to run on final shutdown. This is
    // mostly used by tests.
    private List<Runnable> doFinally= new ArrayList<>();
    
    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void registerInstance() {
        instance = this;
    }
    
    /**
     * Register a {@link Runnable} to execute on the eventual
     * shutdown of the application. While most shutdown procedures
     * should be controlled by a specific service, there are some times
     * when a specific task must be performed after almost every other
     * service has been shutdown. The only current use of this method
     * is to signal deletion of a temporary file after a test is completed
     * AND the application has been shut down. Use of this method is discouraged.
     *  
     * @param r
     */
    public static void addStaticShutdownRunnable(Runnable r) {
        if(instance!=null) {
            instance.addShutdownRunnable(r);
        }else {
            throw new IllegalStateException("no instance of " + StaticContextAccessor.class.getSimpleName() + " is accessible");
        }
    }
    
    /**
     * Register a {@link Runnable} to execute on the eventual
     * shutdown of the application. While most shutdown procedures
     * should be controlled by a specific service, there are some times
     * when a specific task must be performed after almost every other
     * service has been shutdown. The only current use of this method
     * is to signal deletion of a temporary file after a test is completed
     * AND the application has been shut down. Use of this method is discouraged.
     *  
     * @param r
     */
    public void addShutdownRunnable(Runnable r) {
        this.doFinally.add(r);
    }
    
    @PreDestroy
    private void testDestroy() {
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
    
    public static <T> T getBeanQualified(Class<T> clazz, String qualifier) {
        if (instance != null && instance.applicationContext != null) {
            return BeanFactoryAnnotationUtils.qualifiedBeanOfType(instance.applicationContext.getAutowireCapableBeanFactory(), clazz, qualifier);
        }
        return null;
    }
    
    public static EntityManager getEntityManager(String qualifier) {
        return getBeanQualified(EntityManager.class,qualifier);
    }
    
    public static EntityManager getEntityManagerFor(Key k) {
        String qq=DataSourceConfigRegistry.getQualifierFor(k.getEntityInfo().getEntityClass());
        return StaticContextAccessor.getEntityManager(qq);
    }

    public static <T> Optional<T> getOptionalBean(Class<T> clazz) {
        return Optional.ofNullable(getBean(clazz));
    }
}
