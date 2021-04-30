package gsrs;

import gsrs.backup.BackupEventListener;
import gsrs.controller.ExportController;
import gsrs.controller.GsrsWebConfig;
import gsrs.entityProcessor.BasicEntityProcessorConfiguration;
import gsrs.entityProcessor.ConfigBasedEntityProcessorConfiguration;
import gsrs.indexer.ComponentScanIndexValueMakerConfiguration;
import gsrs.repository.BackupRepository;
import gsrs.validator.ConfigBasedValidatorFactoryConfiguration;
import gsrs.validator.ValidatorConfigConverter;
import ix.core.search.text.Lucene4IndexServiceFactory;
import ix.core.search.text.TextIndexerConfig;
import ix.core.search.text.TextIndexerEntityListener;
import ix.core.search.text.TextIndexerFactory;
import ix.core.util.pojopointer.LambdaParseRegistry;
import ix.core.util.pojopointer.URIPojoPointerParser;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

import java.util.ArrayList;
import java.util.List;

public class GsrsBackupSelector implements ImportSelector {
    @Override
    public String[] selectImports(AnnotationMetadata annotationMetadata) {
        //TODO should we enable something to turn on the entity listener?
        return new String[]{
                BackupEventListener.class.getName()};
    }
}
