package gsrs.tasks;

import com.fasterxml.jackson.annotation.JsonProperty;
import gsrs.config.EntityContextLookup;
import gsrs.imports.ConfigBasedGsrsImportAdapterFactoryFactory;
import gsrs.imports.GsrsImportAdapterFactoryFactory;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.stagingarea.model.ImportData;
import gsrs.stagingarea.model.ImportMetadata;
import gsrs.stagingarea.model.KeyValueMapping;
import gsrs.stagingarea.model.MatchableKeyValueTuple;
import gsrs.stagingarea.repository.ImportMetadataRepository;
import gsrs.stagingarea.repository.KeyValueMappingRepository;
import gsrs.stagingarea.service.StagingAreaEntityService;
import gsrs.stagingarea.service.StagingAreaService;
import ix.core.EntityFetcher;
import ix.core.util.EntityUtils;
import ix.core.utils.executor.ProcessListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.lang.reflect.Constructor;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Slf4j
public class CalculateStagingAreaMatchablesScheduledTask extends ScheduledTaskInitializer {

    public static final String STAGING_AREA_LOCATION = "Staging Area";

    @Autowired
    KeyValueMappingRepository keyValueMappingRepository;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Autowired
    ImportMetadataRepository dataRepository;

    @Autowired
    ConfigBasedGsrsImportAdapterFactoryFactory importAdapterFactoryFactory;

    @Autowired
    private GsrsImportAdapterFactoryFactory gsrsImportAdapterFactoryFactory;

    private Integer threadCount = -1;

    @JsonProperty("threadCount")
    public void setThreadCount(Integer count) {
        threadCount=count;
    }

    private final Map<String, StagingAreaService> servicesByContext = new ConcurrentHashMap<>();

    private StagingAreaService getStagingAreaService(String contextName) {
        String contextNameToUse = EntityContextLookup.getContextFromEntityClass(contextName);
        log.trace("supplied contextName: {}, used {}", contextName, contextNameToUse);
        return servicesByContext.computeIfAbsent(contextNameToUse, n-> {
            try {
                return gsrsImportAdapterFactoryFactory.getStagingAreaService(n);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private StagingAreaEntityService getEntityService(String contextName) {
        return getStagingAreaService(contextName).getEntityService(contextName);
    }

    @Override
    public void run(SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l) {
        l.message("Initializing substance matchable processing");
        l.message("Clearing out old values");
        keyValueMappingRepository.deleteByDataLocation(STAGING_AREA_LOCATION);

        ProcessListener listen = ProcessListener.onCountChange((sofar, total) ->{
            if (total != null)
            {
                l.message("Recalculated:" + sofar + " of " + total);
            } else
            {
                l.message("Recalculated:" + sofar);
            }
        });

        listen.newProcess();

        List<UUID> allIDs = dataRepository.getAllRecordIds();
        listen.totalRecordsToProcess(allIDs.size());

        final int parallelism = 4;
        ForkJoinPool forkJoinPool = null;
        try {
            Runnable r = () ->{
                allIDs.parallelStream().forEach( uuid -> {
                    try {
                        log.trace("looking at substance " + uuid);
                        EntityUtils.Key substanceKey = EntityUtils.Key.of(ImportMetadata.class, uuid);
                        Optional<?> retrieved = EntityFetcher.of(substanceKey).getIfPossible();
                        if(retrieved.isPresent()) {
                            ImportMetadata importMetadata = (ImportMetadata) retrieved.get();
                            ImportData relatedData = getStagingAreaService(importMetadata.getEntityClassName()).getImportDataByInstanceIdOrRecordId(uuid.toString(), 0);
                            Object domainObject= getStagingAreaService(importMetadata.getEntityClassName()).deserializeObject(importMetadata.getEntityClassName(), relatedData.getData());
                            List<MatchableKeyValueTuple> matchables = getEntityService(importMetadata.getEntityClassName()).extractKVM(domainObject);
                            List<KeyValueMapping> kvmaps = matchables.stream().map(kv -> {
                                        KeyValueMapping mapping = new KeyValueMapping();
                                        mapping.setKey(kv.getKey());
                                        mapping.setValue(kv.getValue());
                                        mapping.setQualifier(kv.getQualifier());
                                        mapping.setOwner(importMetadata);
                                        mapping.setEntityClass(importMetadata.getEntityClassName());
                                        mapping.setInstanceId(relatedData.getInstanceId());
                                        mapping.setDataLocation(STAGING_AREA_LOCATION);
                                        return mapping;
                                    })
                                    .collect(Collectors.toList());

                            TransactionTemplate tx = new TransactionTemplate(platformTransactionManager);
                            log.trace("got tx " + tx);
                            tx.executeWithoutResult(a->{
                                keyValueMappingRepository.saveAll(kvmaps);
                                keyValueMappingRepository.flush();
                            });

                            listen.recordProcessed(importMetadata);
                        } else {
                            log.warn("error retrieving substance with ID {}", uuid);
                        }
                    } catch (Exception ignore){
                        log.warn("error processing record {}; continuing to next.", uuid);
                        ignore.printStackTrace();
                    }
                });

            };

            if(threadCount!=null && threadCount>0) {
                forkJoinPool = new ForkJoinPool(threadCount);
                forkJoinPool.submit(r).get();
            }else {
                //will use common fork join pool
                r.run();
            }

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            if (forkJoinPool != null) {
                forkJoinPool.shutdown();
            }
        }
    }


    @Override
    public String getDescription() {
        return "Generate 'Matchables' (used in data import) for all records in the staging area";
    }
}
