package gsrs;

import gsrs.security.GsrsLegacyAuthenticationSelector;
import gsrs.security.LegacyAuthenticationConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import( { LegacyAuthenticationConfiguration.class, GsrsLegacyAuthenticationSelector.class})
public @interface EnableGsrsLegacyAuthentication {
}
