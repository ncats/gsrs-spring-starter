package gsrs.assertions;

import org.hamcrest.Matcher;

/**
 * Hamcrest Matchers for common GSRS tests.
 *
 *
 * <pre>
 *     {@code
 * import static org.junit.jupiter.api.Assertions.*;
 * import static org.hamcrest.MatcherAssert.*;
 * import static org.hamcrest.Matchers.*;
 *
 *     }
 *
 *
 * </pre>
 */
public final class GsrsMatchers {
    private GsrsMatchers(){
        //can not instantiate
    }

    /**
     * Creates a {@link Matcher} that will compare all getters
     * that return non-null values in the passed in example
     * to the object under test to make sure all the getters return
     * the same values.  This is useful to provide example
     * specifications for only certain fields to be checked
     * instead of equals() which might check more fields.
     *
     * @param example the example object to use for comparison; can not be null.
     *
     * @param <T>
     * @return a new Matcher
     */
    public static <T> MatchesExample<T> matchesExample(T example){
        return new MatchesExample<>(example);
    }
}
