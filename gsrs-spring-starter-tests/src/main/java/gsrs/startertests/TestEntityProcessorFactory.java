package gsrs.startertests;

import gsrs.AbstractEntityProcessorFactory;
import gsrs.EntityProcessorFactory;
import ix.core.EntityProcessor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Test IndexValueMakerFactory that let's test code directly
 * control which {@link EntityProcessor}s to add.
 */
public class TestEntityProcessorFactory extends AbstractEntityProcessorFactory {

    private List<EntityProcessor> entityProcessors = new ArrayList<>();

    public TestEntityProcessorFactory(EntityProcessor... entityProcessors){
        this.entityProcessors.addAll(Arrays.asList(entityProcessors));
    }
    @Override
    protected void registerEntityProcessor(Consumer<EntityProcessor> registar) {
        for(EntityProcessor e: entityProcessors){
            registar.accept(e);
        }
    }
    /**
     * Add the given EntityProcessor to this Factory.
     * @param entityProcessor the entityProcessor to add can not be null.
     * @return this
     * @throws NullPointerException if parameter is null.
     */
    public TestEntityProcessorFactory addEntityProcessor(EntityProcessor entityProcessor){
        entityProcessors.add(Objects.requireNonNull(entityProcessor));
        resetCache();
        return this;
    }

    /**
     * Set the EntityProcessors to the given list.
     * @param entityProcessors
     * @return this
     * @throws NullPointerException if any entityProcessors are null.
     */
    public TestEntityProcessorFactory setEntityProcessors(EntityProcessor... entityProcessors){
        ArrayList<EntityProcessor> list = new ArrayList<>(Arrays.asList(entityProcessors));
        list.forEach(Objects::requireNonNull);
        this.entityProcessors = list;
        resetCache();
        return this;
    }
    /**
     * Remove all entityProcessors.
     */
    public void clearAll() {
        entityProcessors.clear();
        resetCache();
    }
}
