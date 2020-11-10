package gsrs;

import gsrs.repository.GroupRepository;
import ix.core.models.IxModel;
import ix.ginas.models.GinasCommonData;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import( {GsrsEntitiesConfiguration.class, GsrsJpaEntitySelector.class})
//@EnableJpaRepositories(basePackages = {"gsrs", "gov.nih.ncats"})
public @interface EnableGsrsJpaEntities {
}
