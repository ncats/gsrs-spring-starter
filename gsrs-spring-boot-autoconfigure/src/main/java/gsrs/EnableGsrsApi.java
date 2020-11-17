package gsrs;

import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import( GsrsApiSelector.class)
public @interface EnableGsrsApi {

    /**
     * The way Entities are Text Indexed performed by the GSRS API Controller.
     * Unfortunately due to classpath issues and different incompatible versions
     * of classes, only one `IndexerType` is allowed per microservice.
     * This means all GSRSApiController implementations
     * must all use the same Indexer Type or #NONE.
     */
    enum IndexerType{
        /**
         * Use the same Text Indexer used
         * by the legacy pre-Spring version of GSRS which
         * uses @Indexable annotation and IndexValueMakers
         * to populate a Lucene 4 index.
         */
        LEGACY,
        /**
         * No indexing will be done.
         */
        NONE
        ;
    }

    /**
     * The way {@link ix.core.EntityProcessor}s are detected by this starter package.
     * Only {@link ix.core.EntityProcessor}s that are detected by the Detector implementation
     * will be called when entities CRUD operations are performed.
     */
    enum EntityProcessorDetector {
        /**
         * Use a configuration file to list which {@link ix.core.EntityProcessor}
         * implementations to use.
         */
        CONF,
        /**
         * Add any {@link ix.core.EntityProcessor} discovered by the Spring component scan,
         * this means that EntityProcessor classes must be annotated with @Component.
         */
        COMPONENT_SCAN
        ;
    }

    /**
     * The {@link IndexerType} to use by default uses {@link IndexerType#LEGACY}.
     * @return the {@link IndexerType} can not be null.
     */
    IndexerType indexerType() default IndexerType.LEGACY;

    /**
     * The {@link EntityProcessorDetector} to use, by default uses {@link EntityProcessorDetector#CONF}.
     * @return the {@link EntityProcessorDetector} can not be null.
     */
    EntityProcessorDetector entityProcessorDetector() default EntityProcessorDetector.CONF;
}
