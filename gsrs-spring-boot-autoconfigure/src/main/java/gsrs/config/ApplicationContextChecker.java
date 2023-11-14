package gsrs.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.SingletonBeanRegistry;
import org.springframework.context.ApplicationContext;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Data

public class ApplicationContextChecker {

    public boolean enabled;

    public String getBeanDefinitionNames(ApplicationContext ctx) {
        List<String> omissionFragments = Arrays.asList("password","abc");
        boolean isAdmin = true;
        if (enabled && isAdmin) {
            StringBuilder sb = new StringBuilder();
            sb.append("Beans provided by Spring Boot:\n\n");
            String[] beanNames = ctx.getBeanDefinitionNames();
            Arrays.sort(beanNames);
            for (String beanName : beanNames) {
                sb.append(beanName).append("\n");
            }
            return sb.toString();
        } else {
            return "Unauthorized.";
        }
    }

    public String getSingletonNames(ApplicationContext applicationContext) {
        StringBuilder sb = new StringBuilder();
        AutowireCapableBeanFactory autowireCapableBeanFactory = applicationContext.getAutowireCapableBeanFactory();
        if (autowireCapableBeanFactory instanceof SingletonBeanRegistry) {
            String[] singletonNames = ((SingletonBeanRegistry) autowireCapableBeanFactory).getSingletonNames();
            for (String singleton : singletonNames) {
                sb.append(singleton).append("\n");
            }
        }
        return sb.toString();
    }
}

