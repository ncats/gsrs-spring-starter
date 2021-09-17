package gsrs.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

public class ScheduledTaskStartupRunner implements ApplicationRunner{

    @Autowired
    GsrsSchedulerTaskPropertiesConfiguration config;
    

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // TODO Auto-generated method stub
//        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!Calling startup initializer");
        config.init();
    }

}
