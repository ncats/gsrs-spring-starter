package gsrs.assertions;

import gsrs.junit.TestUtil;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.junit.jupiter.api.Test;



import static org.junit.jupiter.api.Assertions.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class MatcherExampleTest {
    @Data
    @Builder
    @AllArgsConstructor
    public static class Foo{
        private String bar;
        private boolean baz;

    }

    @Test
    public void sameObjectIsEqual(){
        Foo foo = new Foo("myBar", false);
        TestUtil.assertAreEqualsAndHashCodeSame(foo, foo);

        assertThat(foo, GsrsMatchers.matchesExample(foo));

    }

    @Test
    public void sameValuesAreEqual(){
        Foo foo = new Foo("myBar", false);
        Foo foo2 = new Foo("myBar", false);

        TestUtil.assertAreEqualsAndHashCodeSame(foo, foo2);

        assertThat(foo, GsrsMatchers.matchesExample(foo2));
    }

    @Test
    public void exampleMethodsShouldOnlyConsiderNoNullFields(){
        Foo foo = new Foo("myBar", false);
        Foo foo2 = new Foo(null, false);

        TestUtil.assertAreNotEqualsAndHashCodeDifferent(foo, foo2);

        assertThat(foo, GsrsMatchers.matchesExample(foo2));
    }
    @Test
    public void ignoreExplicitField(){
        Foo foo = new Foo("myBar", false);
        Foo foo2 = new Foo("myBar", true);

        TestUtil.assertAreNotEqualsAndHashCodeDifferent(foo, foo2);

        assertThat(foo, GsrsMatchers.matchesExample(foo2)
                                    .ignoreField("baz"));
    }
    @Test
    public void exampleMethodsShouldNotMatchIfFieldsDifferent(){
        Foo foo = new Foo("myBar", false);
        Foo foo2 = new Foo("myBar", true);

        TestUtil.assertAreNotEqualsAndHashCodeDifferent(foo, foo2);

        assertThat(foo, not(GsrsMatchers.matchesExample(foo2)));
    }
    @Test
    public void exampleMethodsShouldOnlyConsiderNoNullFieldsBuilder(){
        Foo foo = new Foo("myBar", false);
        Foo foo2 = Foo.builder().baz(false).build();

        TestUtil.assertAreNotEqualsAndHashCodeDifferent(foo, foo2);

        assertThat(foo, GsrsMatchers.matchesExample(foo2));
    }
    @Test
    public void differentValuesAreNotEqual(){
        Foo foo = new Foo("myBar", false);
        Foo foo2 = new Foo("different", false);

        TestUtil.assertAreNotEqualsAndHashCodeDifferent(foo, foo2);

        assertThat(foo, not(GsrsMatchers.matchesExample(foo2)));
    }

}
