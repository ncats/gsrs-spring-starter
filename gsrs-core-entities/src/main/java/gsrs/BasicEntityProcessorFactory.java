package gsrs;

import ix.core.EntityProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;

@Service
public class BasicEntityProcessorFactory extends  AbstractEntityProcessorFactory {
    @Autowired(required = false)
    private List<EntityProcessor> entityProcessors;


    @Override
    protected void registerEntityProcessor(Consumer<EntityProcessor> registar) {
        if(entityProcessors !=null) {
            for (EntityProcessor ep : entityProcessors) {
                registar.accept(ep);
            }
        }
    }



}
