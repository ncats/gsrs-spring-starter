package gsrs;

import gsrs.scheduler.GsrsSchedulerSelector;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import( { GsrsSchedulerSelector.class})
public @interface EnableGsrsScheduler {
}
