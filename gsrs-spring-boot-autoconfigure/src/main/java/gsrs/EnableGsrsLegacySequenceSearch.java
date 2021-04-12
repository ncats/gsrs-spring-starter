package gsrs;

import gsrs.payload.GsrsLegacyPayloadSelector;
import gsrs.payload.LegacyPayloadConfiguration;
import gsrs.search.SearchResultController;
import gsrs.sequence.search.legacy.GsrsLegacySequenceIndexerSelector;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import( { GsrsLegacySequenceIndexerSelector.class, SearchResultController.class})
public @interface EnableGsrsLegacySequenceSearch {
}
