package gsrs.springUtils;

import org.springframework.beans.BeansException;
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
        applicationContext.getAutowireCapableBeanFactory().autowireBean(bean);
//        System.out.println("after autowire " + bean);
    }

    public static AutowireHelper getInstance() {
        return instance;
    }
}
