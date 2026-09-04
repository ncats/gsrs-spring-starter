package gsrs.coretests;

import ix.core.models.Indexable;
import ix.core.util.EntityUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class TestEntityTraverser {
    @Data
    @Builder
    @Entity
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Foo{

        private String fooName;
        private List<Bar> bars;
        private Baz baz;
        @Indexable
        public List<Bar> getBars() {
            return bars;
        }
        @Indexable
        public Baz getBaz() {
            return baz;
        }
    }
    @Data
    @Builder
    @Entity
    @NoArgsConstructor
    @AllArgsConstructor
    @Indexable
    public static class Bar{
        @Indexable
        public String barName;


    }

    @Data
    @Builder
    @Entity
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Baz{
        private String bazName;
        private List<Bar> bars;
    }

    @Test

    public void simpleTraversal(){
        Foo foo = Foo.builder()
                .fooName("myFoo")
                .build();
        List<Object> parents= new ArrayList<>();
        List<String> paths = new ArrayList<>();
        List<Object> objects= new ArrayList<>();

        EntityUtils.EntityWrapper.of(foo).traverse().execute((parent, path, o)->{
            parents.add(parent==null?null: parent.getValue());
            paths.add(path.toPath());
            objects.add(o==null?null: o.getValue());
        });

        assertEquals(Arrays.asList((Object) null), parents);
        assertEquals(Arrays.asList("root"), paths);
        assertEquals(Arrays.asList(foo), objects);
    }

    @Test
    public void oneBar(){
        Foo foo = Foo.builder()
                .fooName("myFoo")
                .bars(Arrays.asList(Bar.builder().barName("bar1").build()))
                .build();
        List<Object> parents= new ArrayList<>();
        List<String> paths = new ArrayList<>();
        List<Object> objects= new ArrayList<>();

        EntityUtils.EntityWrapper.of(foo).traverse().execute((parent, path, o)->{
            parents.add(parent==null?null: parent.getValue());
            paths.add(path.toPath());
            objects.add(o==null?null: o.getValue());
        });

        assertEquals(Arrays.asList(null, foo), parents);
        assertEquals(Arrays.asList("root", "root_bars"), paths);
        assertEquals(Arrays.asList(foo, foo.getBars().get(0)), objects);
    }

    @Test
    public void twoBars(){
        Foo foo = Foo.builder()
                .fooName("myFoo")
                .bars(Arrays.asList(
                        Bar.builder().barName("bar1").build(),
                        Bar.builder().barName("bar2").build()))
                .build();
        List<Object> parents= new ArrayList<>();
        List<String> paths = new ArrayList<>();
        List<Object> objects= new ArrayList<>();

        EntityUtils.EntityWrapper.of(foo).traverse().execute((parent, path, o)->{
            parents.add(parent==null?null: parent.getValue());
            paths.add(path.toPath());
            objects.add(o==null?null: o.getValue());
        });

        assertEquals(Arrays.asList(null, foo, foo), parents);
        assertEquals(Arrays.asList("root", "root_bars","root_bars"), paths);
        assertEquals(Arrays.asList(foo, foo.getBars().get(0), foo.getBars().get(1)), objects);
    }
}
