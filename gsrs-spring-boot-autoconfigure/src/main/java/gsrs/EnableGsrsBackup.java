package gsrs;

import gsrs.autoconfigure.GsrsApiAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.hateoas.config.EnableHypermediaSupport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import( {GsrsBackupSelector.class})
public @interface EnableGsrsBackup {


}
