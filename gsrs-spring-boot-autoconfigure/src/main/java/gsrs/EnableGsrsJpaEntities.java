package gsrs;



import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import( { GsrsEntitiesConfiguration.class, GsrsJpaEntitySelector.class})
//@EnableJpaRepositories(basePackages = {"ix", "gsrs", "gov.nih.ncats"})
@EnableTransactionManagement(order=1)
public @interface EnableGsrsJpaEntities {
}
