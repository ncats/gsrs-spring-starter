package ix.core.models;

import java.util.function.Predicate;

public interface FacetFilter extends Predicate<FV> {
    boolean accepted(FV fv);

    default boolean test(FV fv){
        return accepted(fv);
    }
}
