package gsrs;


import gsrs.events.listeners.GsrsEditEventListener;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class GsrsJpaEntitySelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        return new String[]{
                GsrsEntitiesConfiguration.class.getName(),
                AuditConfig.class.getName(),
//                BasicEntityProcessorFactory.class.getName(),
                GsrsEntityProcessorListener.class.getName(),

                EntityPersistAdapter.class.getName(),
                OutsideTransactionUtil.class.getName(),
                GsrsEditEventListener.class.getName()
        };
    }
}
