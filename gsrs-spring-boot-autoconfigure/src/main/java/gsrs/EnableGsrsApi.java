package gsrs;

import gsrs.controller.GsrsWebConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import( GsrsApiSelector.class)
public @interface EnableGsrsApi {


    enum IndexerType{
        LEGACY,
        NONE
        ;
    }

    IndexerType indexerType() default IndexerType.LEGACY;
}
