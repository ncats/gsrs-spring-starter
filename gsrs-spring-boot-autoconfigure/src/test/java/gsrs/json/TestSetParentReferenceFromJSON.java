package gsrs.json;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import ix.core.models.Indexable;
import ix.core.models.ParentReference;
import lombok.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.json.JacksonTester;

import javax.persistence.Entity;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


public class TestSetParentReferenceFromJSON {

    @Entity
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)

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
    @Entity
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Indexable
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Bar{

        private String barName;
        @JsonIgnore
        @ParentReference
        @EqualsAndHashCode.Exclude
        @ToString.Exclude
        private Object owner;



    }
    @Entity
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Baz{
        private String bazName;
        private List<Bar> bars;
        @JsonIgnore
        @ParentReference
        @EqualsAndHashCode.Exclude
        @ToString.Exclude
        private Object owner;

        @Indexable
        public List<Bar> getBars() {
            return bars;
        }
    }


    private JacksonTester<Foo> jacksonTester;
    @BeforeEach
    public void setup() {
        ObjectMapper objectMapper = new ObjectMapper();
        JacksonTester.initFields(this, objectMapper);
    }
    @Test
    public void serializeEmptyFoo() throws IOException {
        Foo foo = new Foo();
        String json = jacksonTester.write(foo).getJson();
        assertEquals("{}", json);
    }

    @Test
    public void serializeJustSetNameFoo() throws IOException {
        Foo foo = Foo.builder().fooName("name").build();
        String json = jacksonTester.write(foo).getJson();
        assertEquals("{\"fooName\":\"name\"}", json);
    }
    @Test
    public void serializeWithABazOwnerNotSerialized() throws IOException {
        Foo foo = Foo.builder().baz(
                Baz.builder()
                        .bazName("myBaz").build()).build();
        String json = jacksonTester.write(foo).getJson();
        assertEquals("{\"baz\":{\"bazName\":\"myBaz\"}}", json);

        Foo fromJson = jacksonTester.parse(json).getObject();
        assertNull(fromJson.getBaz().getOwner());
    }

    @Test
    public void serializeWithABazAndSetFixOwner() throws IOException {
        Foo foo = Foo.builder().baz(
                Baz.builder()
                        .bazName("myBaz").build()).build();
        String json = jacksonTester.write(foo).getJson();
        assertEquals("{\"baz\":{\"bazName\":\"myBaz\"}}", json);


        Foo fromJson = jacksonTester.parse(json).getObject();
        assertNull(fromJson.getBaz().getOwner());
        Foo fixed = JsonEntityUtil.fixOwners(fromJson);
        assertEquals(fixed, fixed.getBaz().getOwner());
    }

    @Test
    public void serializeWithABars() throws IOException {
        Foo foo = Foo.builder().bars(
                Arrays.asList(Bar.builder().barName("myBar").build()))
                .build();

        String json = jacksonTester.write(foo).getJson();
        assertEquals("{\"bars\":[{\"barName\":\"myBar\"}]}", json);


        Foo fromJson = jacksonTester.parse(json).getObject();
        assertNull(fromJson.getBars().get(0).getOwner());
        Foo fixed = JsonEntityUtil.fixOwners(fromJson);
        assertEquals(fixed, fixed.getBars().get(0).getOwner());
    }

    @Test
    public void serializeWithMultipleBars() throws IOException {
        Foo foo = Foo.builder().bars(
                Arrays.asList(
                        Bar.builder().barName("myBar").build(),
                        Bar.builder().barName("bar2").build()
                        ))
                .build();

        String json = jacksonTester.write(foo).getJson();
        assertEquals("{\"bars\":[{\"barName\":\"myBar\"},{\"barName\":\"bar2\"}]}", json);



        Foo fromJson = jacksonTester.parse(json).getObject();
        assertNull(fromJson.getBars().get(0).getOwner());
        assertNull(fromJson.getBars().get(1).getOwner());
        Foo fixed = JsonEntityUtil.fixOwners(fromJson);
        assertEquals(fixed, fixed.getBars().get(0).getOwner());
        assertEquals(fixed, fixed.getBars().get(1).getOwner());
    }

    @Test
    public void serializeWithBazAndFooMultipleBars() throws IOException {
        Foo foo = Foo.builder().bars(
                Arrays.asList(
                        Bar.builder().barName("myBar").build(),
                        Bar.builder().barName("bar2").build()
                ))
                .baz(Baz.builder()
                        .bazName("myBaz").build())
                .build();

        String json = jacksonTester.write(foo).getJson();
        assertEquals("{\"bars\":[{\"barName\":\"myBar\"},{\"barName\":\"bar2\"}],\"baz\":{\"bazName\":\"myBaz\"}}", json);


        Foo fromJson = jacksonTester.parse(json).getObject();
        assertNull(fromJson.getBars().get(0).getOwner());
        assertNull(fromJson.getBars().get(1).getOwner());
        Foo fixed = JsonEntityUtil.fixOwners(fromJson);
        assertEquals(fixed, fixed.getBars().get(0).getOwner());
        assertEquals(fixed, fixed.getBars().get(1).getOwner());

        assertEquals(fixed, fixed.getBars().get(1).getOwner());
    }

    @Test
    public void serializeWithABazAndSubBarsAndSetFixOwners() throws IOException {
        Foo foo = Foo.builder().baz(
                Baz.builder()
                        .bazName("myBaz")
                        .bars(Arrays.asList(
                                Bar.builder().barName("myBar").build(),
                                Bar.builder().barName("bar2").build()
                        ))
                        .build()).build();
        String json = jacksonTester.write(foo).getJson();
        assertEquals("{\"baz\":{\"bazName\":\"myBaz\",\"bars\":[{\"barName\":\"myBar\"},{\"barName\":\"bar2\"}]}}", json);


        Foo fromJson = jacksonTester.parse(json).getObject();
        assertNull(fromJson.getBaz().getOwner());
        assertNull(fromJson.getBaz().getBars().get(0).getOwner());
        assertNull(fromJson.getBaz().getBars().get(1).getOwner());
        Foo fixed = JsonEntityUtil.fixOwners(fromJson);
        assertEquals(fixed, fixed.getBaz().getOwner());
        assertEquals(fixed.getBaz(), fixed.getBaz().getBars().get(0).getOwner());
        assertEquals(fixed.getBaz(), fixed.getBaz().getBars().get(1).getOwner());
    }
}
