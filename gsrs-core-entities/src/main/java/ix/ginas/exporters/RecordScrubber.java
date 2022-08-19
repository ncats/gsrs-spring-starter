package ix.ginas.exporters;

import ix.ginas.models.GinasCommonData;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/*
As of 19 August, this is a shot in the dark
 */
public interface RecordScrubber<T> {
    Optional<T> scrub(T object);

    default Function<T,Optional<T>> filter(T object, Set<String> groups) {
        if(object instanceof GinasCommonData){
            GinasCommonData gsrsObject = (GinasCommonData)object;
            if( gsrsObject.getAccess().stream().anyMatch(g->groups.stream().anyMatch(g2->g2.equalsIgnoreCase(g.name)))) {
                return f->Optional.of(f);
            }
        }
        return f->Optional.empty();
    }
}
