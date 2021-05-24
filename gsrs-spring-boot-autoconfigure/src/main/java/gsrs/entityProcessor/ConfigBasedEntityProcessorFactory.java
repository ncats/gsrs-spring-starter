package gsrs.entityProcessor;

import gsrs.AbstractEntityProcessorFactory;
import gsrs.GsrsFactoryConfiguration;
import ix.core.EntityProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;


@Slf4j
public class ConfigBasedEntityProcessorFactory extends AbstractEntityProcessorFactory {


    @Autowired
    private GsrsFactoryConfiguration config;

    @Override
    protected void registerEntityProcessor(Consumer<EntityProcessor> registar) {
        List<EntityProcessorConfig> eps = config.getEntityProcessors();
        if(eps !=null){
            for(EntityProcessorConfig config: eps){
                try {
                    EntityProcessor newEntityProcessorInstance = config.createNewEntityProcessorInstance();
                    //can't autowire here because the depenendencies might not be set yet!!
//                    AutowireHelper.getInstance().autowire(newEntityProcessorInstance);
                    registar.accept(newEntityProcessorInstance);
                } catch (Exception e) {
                   log.error("could not create new entity Processor instance " + config, e);
                }
            }
        }
    }


}
