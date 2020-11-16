package gsrs.entityProcessor;

import gsrs.GsrsFactoryConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EntityProcessorFactory {

    @Autowired
    private GsrsFactoryConfiguration config;

}
