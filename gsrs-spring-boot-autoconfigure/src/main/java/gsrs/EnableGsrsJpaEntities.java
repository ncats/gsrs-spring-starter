package gsrs;

import ix.core.models.IxModel;
import ix.ginas.models.GinasCommonData;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import( GsrsJpaEntitySelector.class)
@EntityScan(basePackageClasses = {IxModel.class, GinasCommonData.class})
public @interface EnableGsrsJpaEntities {
}
