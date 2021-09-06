package gsrs.springUtils;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class AutowireHelper implements ApplicationContextAware {

    private static AutowireHelper instance;

    private ApplicationContext applicationContext;

    public AutowireHelper(){
        instance = this;
    }
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Use Spring's dependency injection to only set any fields
     * in this bean that are annotated as  @{@link org.springframework.beans.factory.annotation.Autowire},
     * this does NOT make any Proxy objects to handle things like Transactional.
     *
     * @param bean the bean to autowire- can not be null.
     */
    public void autowire(Object bean){
        applicationContext.getAutowireCapableBeanFactory().autowireBean(bean);
    }

    /**
     * Use Spring's AOP Proxy system to make a proxy of the given bean to support things
     * like Transactional AND also inject any dependencies from fields
     * that are annotated with @{@link org.springframework.beans.factory.annotation.Autowire}.
     * This should be equivalent to creating a Spring Bean in a Configuration except the newly created
     * proxy is not added to Spring's Application Context.
     * @param bean the bean to autowire and Proxy; can not be null.
     * @param <T> the type of the Object and is only used for compiler infercing to let you not
     *           have to cast the returned proxy object's type.
     * @return a new Proxy object.
     * @throws NullPointerException if bean is null.
     */
    public <T> T autowireAndProxy(T bean){
        return autowireAndProxy(bean, bean.getClass().getSimpleName());
    }
    /**
     * Use Spring's AOP Proxy system to make a proxy of the given bean to support things
     * like Transactional AND also inject any dependencies from fields
     * that are annotated with @{@link org.springframework.beans.factory.annotation.Autowire}.
     * This should be equivalent to creating a Spring Bean in a Configuration except the newly created
     * proxy is not added to Spring's Application Context.
     * @param bean the bean to autowire and Proxy.
     * @param beanName the name of the bean.
     * @param <T> the type of the Object and is only used for compiler infercing to let you not
     *           have to cast the returned proxy object's type.
     * @return a new Proxy object.
     */
    public <T> T autowireAndProxy(T bean, String beanName){
        AutowireCapableBeanFactory autowireCapableBeanFactory = applicationContext.getAutowireCapableBeanFactory();
        autowireCapableBeanFactory.autowireBean(bean);
        return (T) autowireCapableBeanFactory.applyBeanPostProcessorsAfterInitialization(bean, beanName);
    }


    public ClassLoader getClassLoader(){
        return applicationContext.getClassLoader();
    }
    public static AutowireHelper getInstance() {
        return instance;
    }
}
