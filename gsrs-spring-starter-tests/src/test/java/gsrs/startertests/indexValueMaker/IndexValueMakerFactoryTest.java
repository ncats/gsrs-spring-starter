package gsrs.startertests.indexValueMaker;

import gsrs.AuditConfig;
import gsrs.indexer.IndexValueMakerFactory;
import gsrs.springUtils.AutowireHelper;
import gsrs.startertests.*;
import gsrs.startertests.processors.EntityProcessorTest;
import ix.core.search.text.IndexValueMaker;
import ix.core.search.text.IndexableValue;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@GsrsJpaTest(classes =GsrsSpringApplication.class)

@Import(IndexValueMakerFactoryTest.MyConfig.class)
public class IndexValueMakerFactoryTest {

    @TestConfiguration
    static class MyConfig{
        @Bean
        @Primary
        public IndexValueMakerFactory indexValueMakerFactory(){
            return new TestIndexValueMakerFactory(new MyIndexValueMaker());
        }
    }
    @Autowired
    private IndexValueMakerFactory factory;

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
    @Component
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
