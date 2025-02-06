package gsrs.startertests.indexValueMaker;

import gsrs.startertests.GsrsSpringApplication;
import gsrs.startertests.*;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@GsrsJpaTest(classes = GsrsSpringApplication.class)
@ActiveProfiles("test")
public class IndexValueMakerFactoryTest extends AbstractGsrsJpaEntityJunit5Test {


    @Autowired
    private TestIndexValueMakerFactory factory;

    @BeforeEach
    public void init(){
        factory.setIndexValueMakers(new MyIndexValueMaker());
    }
    @Test
    public void pickedUpComponent(){
        Foo foo = new Foo();
        foo.setBar("exampleBar");

        IndexValueMaker<Foo> ivm = factory.createIndexValueMakerFor(foo);
        List<String> results = new ArrayList<>();
        ivm.createIndexableValues(foo, iv->{
            if("myCustomKey".equals(iv.name())){
                results.add((String) iv.value());
            }
        });

        assertEquals(Collections.singletonList("exampleBar"), results);
    }

    @Test
    public void pickedUpSubclassesComponent(){
        SubFoo subFoo = new SubFoo();
        subFoo.setBar("exampleBar");
        subFoo.setAnotherField("something completely different");

        IndexValueMaker<SubFoo> ivm = factory.createIndexValueMakerFor(subFoo);
        List<String> results = new ArrayList<>();
        ivm.createIndexableValues(subFoo, iv->{
            if("myCustomKey".equals(iv.name())){
                results.add((String) iv.value());
            }
        });

        assertEquals(Collections.singletonList("exampleBar"), results);
    }
    @Test
    public void onlyUsesValueMakersThatMatchEntity(){
        NotFoo notFoo = new NotFoo();
        notFoo.setBar("exampleBar");

        IndexValueMaker<NotFoo> ivm = factory.createIndexValueMakerFor(notFoo);
        List<String> results = new ArrayList<>();
        ivm.createIndexableValues(notFoo, iv->{
            if("myCustomKey".equals(iv.name())){
                results.add((String) iv.value());
            }
        });

        assertTrue(results.isEmpty());
    }

    @Data
    public static class NotFoo{
        private String bar;
    }
    @Data
    public static class Foo{
        private String bar;
    }
    @Data
    public static class SubFoo extends Foo{
        private String anotherField;
    }

    public static class MyIndexValueMaker implements IndexValueMaker<Foo> {

        @Override
        public Class<Foo> getIndexedEntityClass() {
            return Foo.class;
        }

        @Override
        public void createIndexableValues(Foo foo, Consumer<IndexableValue> consumer) {
            consumer.accept(IndexableValue.simpleStringValue("myCustomKey", foo.getBar()));
        }
    }
}
