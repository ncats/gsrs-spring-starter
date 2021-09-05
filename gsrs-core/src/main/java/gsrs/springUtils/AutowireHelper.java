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

    public void autowire(Object bean){
//        System.out.println("before autowire " + bean);
        AutowireCapableBeanFactory autowireCapableBeanFactory = applicationContext.getAutowireCapableBeanFactory();
        autowireCapableBeanFactory.autowireBean(bean);
//        System.out.println("after autowire " + bean);
    }
    public <T> T autowireAndProxy(T bean){
        return autowireAndProxy(bean, bean.getClass().getSimpleName());
    }
    public <T> T autowireAndProxy(T bean, String beanName){
//        System.out.println("before autowire " + bean);
        AutowireCapableBeanFactory autowireCapableBeanFactory = applicationContext.getAutowireCapableBeanFactory();
        autowireCapableBeanFactory.autowireBean(bean);
        return (T) autowireCapableBeanFactory.applyBeanPostProcessorsAfterInitialization(bean, beanName);
//        System.out.println("after autowire " + bean);
    }


    public ClassLoader getClassLoader(){
        return applicationContext.getClassLoader();
    }
    public static AutowireHelper getInstance() {
        return instance;
    }
}
