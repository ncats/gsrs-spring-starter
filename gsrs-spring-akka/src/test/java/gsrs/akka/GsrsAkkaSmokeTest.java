package gsrs.akka;

import akka.actor.Actor;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.StaticApplicationContext;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class GsrsAkkaSmokeTest {

    @Test
    void springActorProducerCreatesActorFromSpringContext() {
        StaticApplicationContext context = new StaticApplicationContext();
        Actor dummyActor = (Actor) Proxy.newProxyInstance(
                Actor.class.getClassLoader(),
                new Class<?>[]{Actor.class},
                (proxy, method, args) -> null);
        context.getDefaultListableBeanFactory().registerSingleton("dummyActor", dummyActor);

        SpringActorProducer producer = new SpringActorProducer(context, "dummyActor");

        Actor actor = producer.produce();

        assertSame(dummyActor, actor);
        assertEquals(dummyActor.getClass(), producer.actorClass());
    }
}
