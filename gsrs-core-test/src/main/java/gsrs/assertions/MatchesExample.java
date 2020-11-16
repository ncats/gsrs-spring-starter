package gsrs.assertions;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.util.*;

/**
 *
 * @param <T>
 */
public final class MatchesExample<T> extends TypeSafeMatcher<T> {

    private final T expected;
    private Map<String, Object> expectedValues;

    private Map<String, Object> mismatchedFields;


    public MatchesExample(T expected) {
        super(expected.getClass());
        this.expected = expected;
        expectedValues = getNonNullFieldValues(expected);
    }

    /**
     * Ignore the field with the given field name.
     * @param fieldName
     * @return
     */
    public MatchesExample<T> ignoreField(String fieldName){
        expectedValues.remove(fieldName);
        return this;
    }




    public static Map<String, Object> getMismatchedFields(Object bean, Map<String, Object> expectedValues) {
        try {
            Map<String, Object> map = new HashMap<>();
            Arrays.asList(Introspector.getBeanInfo(bean.getClass(), Object.class)
                    .getPropertyDescriptors())
                    .stream()
                    // filter out properties with setters only
                    .filter(pd -> Objects.nonNull(pd.getReadMethod()) && expectedValues.containsKey(pd.getName()))
                    .forEach(pd -> { // invoke method to get value
                        try {
                            Object value = pd.getReadMethod().invoke(bean);
                            if (!expectedValues.get(pd.getName()).equals(value)) {
                                map.put(pd.getName(), value);
                            }
                        } catch (Exception e) {
                            // add proper error handling here
                        }
                    });
            return map;
        } catch (IntrospectionException e) {
            // and here, too
            return Collections.emptyMap();
        }
    }
    public static Map<String, Object> getNonNullFieldValues(Object bean) {
        try {
            Map<String, Object> map = new HashMap<>();
            Arrays.asList(Introspector.getBeanInfo(bean.getClass(), Object.class)
                    .getPropertyDescriptors())
                    .stream()
                    // filter out properties with setters only
                    .filter(pd -> Objects.nonNull(pd.getReadMethod()))
                    .forEach(pd -> { // invoke method to get value
                        try {
                            Object value = pd.getReadMethod().invoke(bean);
                            if (value != null) {
                                map.put(pd.getName(), value);
                            }
                        } catch (Exception e) {
                            // add proper error handling here
                        }
                    });
            return map;
        } catch (IntrospectionException e) {
            // and here, too
            return Collections.emptyMap();
        }
    }

    @Override
    protected boolean matchesSafely(T item) {
        mismatchedFields = getMismatchedFields(item, expectedValues);
        return mismatchedFields.isEmpty();
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("non null getters return same non-null values");
    }
}
