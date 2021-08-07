package gsrs;

import gsrs.services.BackupService;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import gsrs.backup.BackupEventListener;

public class GsrsBackupSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        //TODO should we enable something to turn on the entity listener?
        return new String[]{
                BackupService.class.getName(),
                BackupEventListener.class.getName()};
    }
}
