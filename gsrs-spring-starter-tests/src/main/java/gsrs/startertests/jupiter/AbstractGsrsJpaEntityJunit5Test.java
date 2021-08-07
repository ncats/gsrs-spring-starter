package gsrs.startertests.jupiter;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.test.context.ContextConfiguration;

import gov.nih.ncats.common.io.IOUtil;
import gsrs.AuditConfig;
import gsrs.EntityPersistAdapter;
import gsrs.cache.GsrsCache;
import gsrs.cache.GsrsLegacyCacheConfiguration;
import gsrs.cache.GsrsLegacyCachePropertyConfiguration;
import gsrs.payload.LegacyPayloadConfiguration;
import gsrs.payload.LegacyPayloadService;
import gsrs.repository.FileDataRepository;
import gsrs.repository.PayloadRepository;
import gsrs.springUtils.AutowireHelper;
import gsrs.springUtils.StaticContextAccessor;
import ix.core.cache.IxCache;

/**
 * Abstract class that autoregisters some GSRS Junit 5 Extensions (what Junit 4 called "Rules")
 * such as {@link ClearIxHomeExtension} and {@link ClearAuditorBeforeEachExtension}.  This also changes
 * the property for `ix.home` which is used by the LegacyTextIndexer to
 * make the TextIndexer write the index to a temporary folder for each test.
 */
@ContextConfiguration(initializers = AbstractGsrsJpaEntityJunit5Test.Initializer.class)
@Import({ClearAuditorBeforeEachExtension.class , 
//    ClearIxHomeExtension.class,  
    AuditConfig.class, AutowireHelper.class,
        EntityPersistAdapter.class, EntityPersistAdapter.class, AbstractGsrsJpaEntityJunit5Test.PayloadTestConf.class,
        ResetAllCacheSupplierBeforeEachExtension.class, ResetAllCacheSupplierBeforeAllExtension.class,
        ResetAllEntityProcessorBeforeEachExtension.class, ResetAllEntityProcessorBeforeAllExtension.class,
        ResetAllEntityServicesBeforeEachExtension.class, ResetAllEntityServicesBeforeEachExtension.class,
        ClearDeserializerCachesBeforeEachExtension.class,

})

public abstract class AbstractGsrsJpaEntityJunit5Test {

   // TP: @TempDir actually deletes at the wrong time, since it's
   //     after the TEST but before the application is shutdown
   //     this leads to errors about textindex/structureindex/sequenceindexer
   //     not finding files they already have open.
    
//    @TempDir
    protected static File tempDir;
    
    // TP: these 2 methods make up for the shortcomings
    // of @TempDir
    
    @BeforeAll
    public static void loadFile() throws IOException {
        tempDir = Files.createTempDirectory("gsrs").toFile();
    }
   
    
    @AfterEach
    public void deleteFile() throws IOException {
        StaticContextAccessor.addStaticShutdownRunnable(()->{
            IOUtil.deleteRecursivelyQuitely(tempDir);
        });
    }

//    @MockBean
    @Autowired
    protected EntityPersistAdapter epa;

//    @Autowired
//    @RegisterExtension
//    protected ClearIxHomeExtension clearTextIndexerRule;

    @Autowired
    @RegisterExtension
    protected ClearAuditorBeforeEachExtension clearAuditorRule;

    @Autowired
    private LegacyPayloadConfiguration legacyPayloadConfiguration;

    @Autowired
    @RegisterExtension
    protected ClearDeserializerCachesBeforeEachExtension clearDeserializerCaches;


    @BeforeEach
    public void createPayloadDir() throws IOException {
        
        //TP: the following snippet was also problematic since it would
        // delete files AFTER the application got running
        // instead, it's probably better to remove only at shutdown each time
        
        //the static reference to the temp dir saves the directory across
        //tests.  We only make it static so that we can reference it in the
        //Initializers to override ix.home
        //so manually delete it and re-create it
//        if(tempDir.exists()){
//            IOUtil.deleteRecursivelyQuitely(tempDir);
//            tempDir.mkdirs();
//        }
        
        
        if(! legacyPayloadConfiguration.getBase().exists()){
            Files.createDirectories(legacyPayloadConfiguration.getBase().toPath());
        }
    }
    

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues.of(
                    "ix.home=" + tempDir
            ).applyTo(context);
        }
    }

    @TestConfiguration
    public static class PayloadTestConf {

        @Autowired
        private PayloadRepository payloadRepository;
        @Autowired
        private FileDataRepository fileDataRepository;

        @Primary //for testing we want this to take precendence
        @Bean
        public LegacyPayloadConfiguration legacyPayloadConfiguration() throws IOException {
            LegacyPayloadConfiguration conf = new LegacyPayloadConfiguration();
            File payloadDir = new File(tempDir, "payload");

            Files.createDirectories(payloadDir.toPath());
            conf.setBase(payloadDir);
            return conf;
        }
        @ConditionalOnMissingBean
        @Bean
        public LegacyPayloadService legacyPayloadService( LegacyPayloadConfiguration conf) throws IOException {
            return new LegacyPayloadService(payloadRepository, conf, fileDataRepository);
        }

        @ConditionalOnMissingBean
        @Bean
        @Primary
        @Order
        public GsrsLegacyCachePropertyConfiguration gsrsLegacyCachePropertyConfiguration(){
            GsrsLegacyCachePropertyConfiguration conf = new GsrsLegacyCachePropertyConfiguration();
            conf.setBase(new File(tempDir, "cache").getAbsolutePath());
            conf.setDebugLevel(5);
            return conf;
        }

        @Bean
        @ConditionalOnMissingBean
        @Order
        public GsrsLegacyCacheConfiguration legacyGsrsCache(GsrsLegacyCachePropertyConfiguration conf){
            return new GsrsLegacyCacheConfiguration(conf);

        }

        @Bean
        @ConditionalOnMissingBean(GsrsCache.class)
        @Order
        public GsrsCache gsrsCache(GsrsLegacyCachePropertyConfiguration conf){
            return new IxCache(conf.createNewGateKeeper(), conf);
        }

    }
}
