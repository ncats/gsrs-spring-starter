package gsrs.assertions;

import gsrs.model.MatchingIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Matches the given example object by going through the getters
 * and comparing only the getter return values that don't return {@code null}.
 * Some fields may be ignored even if they don't return null by either
 * annotating the getter method on the Bean with {@link MatchingIgnore}
 * or by explicitly mentioning the bean field name
 * with {@link #ignoreField(String)}.
 *
 * @param <T>
 */
public final class MatchesExample<T> extends TypeSafeMatcher<T> {

    private final T expected;
    private Map<String, Object> expectedValues;

    private List<MismatchField> mismatchedFields;


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


    @Data
    @AllArgsConstructor
    public static class MismatchField{
        private String fieldName;
        private Object actual, expected;
    }

    public static List<MismatchField> getMismatchedFields(Object bean, Map<String, Object> expectedValues) {
        try {
            List<MismatchField> mismatchFields = new ArrayList<>();
            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass(), Object.class);

            Arrays.asList(beanInfo
                    .getPropertyDescriptors())
                    .stream()
                    // filter out properties with setters only
                    .filter(pd -> Objects.nonNull(pd.getReadMethod()) && expectedValues.containsKey(pd.getName()))
                    .forEach(pd -> { // invoke method to get value
                        try {

                            Method readMethod = pd.getReadMethod();
                            //set accessible so we can invoke it if it's a private getter
                            //or more likely a non-public class
                            readMethod.setAccessible(true);
                            Object value = readMethod.invoke(bean);
                            Object o = expectedValues.get(pd.getName());
                            if (!o.equals(value)) {
                                mismatchFields.add(new MismatchField(pd.getName(), value, o));

                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            // add proper error handling here
                        }
                    });
            return mismatchFields;
        } catch (IntrospectionException e) {
            // and here, too
            return Collections.emptyList();
        }
    }
    public static Map<String, Object> getNonNullFieldValues(Object bean) {
        try {
            Map<String, Object> map = new HashMap<>();
            BeanInfo beanInfo = Introspector.getBeanInfo(bean.getClass(), Object.class);
            Arrays.asList(beanInfo
                    .getPropertyDescriptors())
                    .stream()
                    // filter out properties with setters only
                    .filter(pd -> Objects.nonNull(pd.getReadMethod()))
//                    .filter(pd-> pd.)
                    .forEach(pd -> { // invoke method to get value
                        try {
                            Method readMethod = pd.getReadMethod();
                            //set accessible so we can invoke it if it's a private getter
                            //or more likely a non-public class
                            readMethod.setAccessible(true);
                            if(readMethod.getAnnotation(MatchingIgnore.class) ==null) {
                                Object value = readMethod.invoke(bean);
                                if (value != null) {
                                    map.put(pd.getName(), value);
                                }
                            }
                        } catch (Exception e) {
                            // add proper error handling here
                            e.printStackTrace();
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
        description.appendText("non null getters return same non-null values: " + mismatchedFields);
    }
}
