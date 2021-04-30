package gsrs.scheduler;

import gsrs.controller.ScheduledTaskController;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class GsrsSchedulerSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        return new String[]{
                ScheduledTaskController.class.getName(),
                GsrsSchedulerConfiguration.class.getName(),
        };
    }
}
