package gsrs;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import gsrs.backup.BackupEventListener;

public class GsrsBackupSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        //TODO should we enable something to turn on the entity listener?
        return new String[]{
                BackupEventListener.class.getName()};
    }
}
