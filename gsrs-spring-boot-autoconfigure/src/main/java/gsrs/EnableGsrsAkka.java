package gsrs;

import gsrs.akka.GsrsAkkaSelector;
import gsrs.sequence.search.legacy.GsrsLegacySequenceIndexerSelector;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import( { GsrsAkkaSelector.class})
public @interface EnableGsrsAkka {
}
