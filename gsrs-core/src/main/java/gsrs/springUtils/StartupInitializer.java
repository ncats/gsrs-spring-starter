package gsrs.springUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import ix.core.initializers.GsrsInitializerPropertiesConfiguration;

@Profile("!test")
@Component
public class StartupInitializer implements ApplicationRunner {

    @Autowired
    private GsrsInitializerPropertiesConfiguration gsrsInitalizerConfiguration;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        // TODO Add config-based intializers here, as previously found in Play code
        //
        gsrsInitalizerConfiguration.getInitializers().forEach(init->{
            System.out.println("Running init");
            init.onStart(args);
        });
        
    }

}
