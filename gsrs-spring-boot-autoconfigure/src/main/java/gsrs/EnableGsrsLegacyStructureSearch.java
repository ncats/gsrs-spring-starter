package gsrs;

import gsrs.search.SearchResultController;
import gsrs.sequence.search.legacy.GsrsLegacySequenceIndexerSelector;
import gsrs.structure.legacy.GsrsLegacyStructureIndexerSelector;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import( { GsrsLegacyStructureIndexerSelector.class, SearchResultController.class})
public @interface EnableGsrsLegacyStructureSearch {
}
