package gsrs;

import gsrs.payload.GsrsLegacyPayloadSelector;
import gsrs.payload.LegacyPayloadConfiguration;
import gsrs.security.GsrsLegacyAuthenticationSelector;
import gsrs.security.LegacyAuthenticationConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import( { LegacyPayloadConfiguration.class, GsrsLegacyPayloadSelector.class})
public @interface EnableGsrsLegacyPayload {
}
