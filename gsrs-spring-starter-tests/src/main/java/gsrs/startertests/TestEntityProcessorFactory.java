package gsrs.startertests;

import gsrs.AbstractEntityProcessorFactory;
import gsrs.EntityProcessorFactory;
import ix.core.EntityProcessor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Component
public class TestEntityProcessorFactory extends AbstractEntityProcessorFactory {

    List<EntityProcessor> entityProcessors;
    public TestEntityProcessorFactory(EntityProcessor... entityProcessors){
        this.entityProcessors = Arrays.asList(entityProcessors);
    }
    @Override
    protected void registerEntityProcessor(Consumer<EntityProcessor> registar) {
        for(EntityProcessor e: entityProcessors){
            registar.accept(e);
        }
    }
}
