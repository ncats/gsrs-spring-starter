package gsrs.startertests.processors;

import gsrs.startertests.GsrsSpringApplication;
import gsrs.model.AbstractGsrsEntity;
import gsrs.repository.PrincipalRepository;

import gsrs.startertests.*;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.EntityProcessor;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.verify;

import static org.junit.jupiter.api.Assertions.assertEquals;

@GsrsJpaTest(classes ={GsrsSpringApplication.class})
@ActiveProfiles("test")
public class EntityProcessorTest  extends AbstractGsrsJpaEntityJunit5Test {
    @Data
    @Entity
    @EqualsAndHashCode(callSuper=false)
    public static class MyEntity extends AbstractGsrsEntity {
        @Id
        @GeneratedValue
        private Long id;

        private String foo;
    }



    private static List<String> list = new ArrayList<>();

    @BeforeEach
    public void clearList(){
        entityProcessorFactory.setEntityProcessors(new MyEntityProcessor());
        list.clear();

    }

//    @RegisterExtension
//    ResetAllEntityProcessorBeforeEachExtension resetAllEntityProcessorBeforeEachExtension = new ResetAllEntityProcessorBeforeEachExtension();

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PrincipalRepository principalRepository;


    @Autowired
    private TestEntityProcessorFactory entityProcessorFactory;
    @Test
    public void persist(){
        MyEntity sut = new MyEntity();

        sut.setFoo("myDomain");

        entityManager.persistAndFlush(sut);

        assertEquals(Arrays.asList("prePersist myDomain", "postPersist myDomain"), list);

    }

    @Test
    public void postLoad(){
        MyEntity sut = new MyEntity();

        sut.setFoo("myDomain");

        entityManager.persistAndFlush(sut);

//        sut=null;
        list.clear();
        //for some reason the entityManager.find() isn't having postLoad called, but refresh is so we will use that
        entityManager.refresh(sut);
        assertEquals("myDomain", sut.getFoo());
        assertEquals(Arrays.asList("postLoad myDomain"), list);

    }

    @Test
    public void update(){
        MyEntity sut = new MyEntity();

        sut.setFoo("myDomain");

        sut = entityManager.persistAndFlush(sut);

        list.clear();
        sut.setFoo("domain2");

//        when(epa.preUpdateBeanDirect(eq(sut),any() )).thenAnswer(invocation->{
//            ((Unchecked.ThrowingRunnable) invocation.getArgument(1)).run();
//            return null;
//        });
//       doAnswer(invocation->{
//            ((Unchecked.ThrowingRunnable) invocation.getArgument(3)).run();
//            return null;
//        }).when(epa).postUpdateBeanDirect(eq(sut), any(), eq(true),any() );

        MyEntity sut2 =  entityManager.persistAndFlush(sut);


        assertEquals(Arrays.asList("preUpdate domain2", "postUpdate domain2"), list);

    }

    @Test
    public void remove(){
        MyEntity sut = new MyEntity();

        sut.setFoo("myDomain");

        sut = entityManager.persistAndFlush(sut);

        list.clear();

       entityManager.remove(sut);
        assertEquals(Arrays.asList("preRemove myDomain"), list);
        //postRemove isn't called until flush
        entityManager.flush();

        assertEquals(Arrays.asList("preRemove myDomain", "postRemove myDomain"), list);

    }

    @Component
    public static class MyEntityProcessor implements EntityProcessor<MyEntity> {

        @Override
        public void prePersist(MyEntity obj) {
            list.add("prePersist " + obj.getFoo());
        }

        @Override
        public void postPersist(MyEntity obj) throws FailProcessingException {
            list.add("postPersist " + obj.getFoo());
        }

        @Override
        public void preRemove(MyEntity obj) throws FailProcessingException {
            list.add("preRemove " + obj.getFoo());
        }

        @Override
        public void postRemove(MyEntity obj) throws FailProcessingException {
            list.add("postRemove " + obj.getFoo());
        }

        @Override
        public void preUpdate(MyEntity obj) throws FailProcessingException {
            list.add("preUpdate " + obj.getFoo());
        }

        @Override
        public void postUpdate(MyEntity obj) throws FailProcessingException {
            list.add("postUpdate " + obj.getFoo());
        }

        @Override
        public void postLoad(MyEntity obj) throws FailProcessingException {
            list.add("postLoad " + obj.getFoo());
        }

        @Override
        public Class<MyEntity> getEntityClass() {
            return MyEntity.class;
        }
    }
}
