package gsrs.scheduler;

import gsrs.scheduler.controller.ScheduledTaskController;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class GsrsSchedulerSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        return new String[]{
                GsrsSchedulerConfiguration.class.getName(),
                ScheduledTaskController.class.getName(),
                GsrsSchedulerTaskPropertiesConfiguration.class.getName(),
                ScheduledTaskStartupRunner.class.getName(),
        };
    }
}
