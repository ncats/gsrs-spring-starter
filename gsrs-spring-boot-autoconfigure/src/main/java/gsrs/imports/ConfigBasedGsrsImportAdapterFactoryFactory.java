package gsrs.imports;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.GsrsFactoryConfiguration;
import gsrs.dataexchange.model.ProcessingAction;
import gsrs.springUtils.AutowireHelper;
import gsrs.stagingarea.service.StagingAreaEntityService;
import gsrs.stagingarea.service.StagingAreaService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class ConfigBasedGsrsImportAdapterFactoryFactory implements GsrsImportAdapterFactoryFactory {

    @Autowired
    private GsrsFactoryConfiguration gsrsFactoryConfiguration;

    private final static Map<String, Class<T>> serviceMap = new HashMap<>();

    private final static Map<String, StagingAreaService> serviceInstanceMap = new HashMap<>();

    private static final List<ProcessingAction<T>> processingActionClasses =new ArrayList<>();

    @Override
    public <T> List<ImportAdapterFactory<T>> newFactory(String context, Class <T> clazz) {
        log.trace("newFactory.  clazz: " + clazz.getName());
        List<? extends ImportAdapterFactoryConfig> configs = gsrsFactoryConfiguration.getImportAdapterFactories(context);
        ObjectMapper mapper = new ObjectMapper();
        return configs.stream().map(c ->
                {
                    try {
                        ImportAdapterFactory<T> iaf = (ImportAdapterFactory<T>) c.newImportAdapterFactory(mapper, AutowireHelper.getInstance().getClassLoader());
//                        log.trace("c.getStagingServiceClass(): {}", c.getStagingAreaServiceClass()==null ? "null!!" :
//                                c.getStagingAreaServiceClass().getName());
                        iaf.setStagingAreaService(c.getStagingAreaServiceClass());
                        //log.trace("entity services:");
                        //c.getEntityServices().forEach(k->log.trace("k: {} ", k.getName()));
                        //iaf.setEntityServices(c.getEntityServices());
                        iaf.setEntityServiceClass(c.getEntityServiceClass());
                        iaf = AutowireHelper.getInstance().autowireAndProxy(iaf);

                        //allow config to override what's in code
                        if(c.getDescription() !=null && c.getDescription().length()>0) {
                            iaf.setDescription(c.getDescription());
                        }
                        //log.trace("using description {} for this iaf", iaf.getDescription());
                        if(c.getSupportedFileExtensions()!=null && !c.getSupportedFileExtensions().isEmpty()) {
                            //log.trace("passing on extensions from config: {}", String.join("***", c.getSupportedFileExtensions()));
                            iaf.setSupportedFileExtensions(c.getSupportedFileExtensions());
                        }
                        //TODO initialize throws IllegalStateException should we catch it and report it somewhere?
                        iaf.initialize();

                        return iaf;
                    } catch (Exception e) {
                        log.warn("exception during import adapter factory init: ", e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAvailableAdapterNames(String context) {
        List<? extends ImportAdapterFactoryConfig> configs = gsrsFactoryConfiguration.getImportAdapterFactories(context);
        return configs.stream().map(ImportAdapterFactoryConfig::getAdapterName)
                .collect(Collectors.toList());
    }

    @Override
    public <T> List<ClientFriendlyImportAdapterConfig> getConfiguredAdapters(String context, Class <T> clazz) {
        List<? extends ImportAdapterFactoryConfig> configs = gsrsFactoryConfiguration.getImportAdapterFactories(context);
        ObjectMapper mapper = new ObjectMapper();
        return configs.stream().map(c ->
                {
                    try {
                        ImportAdapterFactory<T> iaf = (ImportAdapterFactory<T>) c.newImportAdapterFactory(mapper, AutowireHelper.getInstance().getClassLoader());
                        log.trace("c.getAdapterName(): {}", c.getAdapterName() );
                        iaf.setStagingAreaService(c.getStagingAreaServiceClass());
                        log.trace("entity services:");
                        //c.getEntityServices().forEach(k->log.trace("k: {} ", k.getName()));
                        //iaf.setEntityServices(c.getEntityServices());
                        iaf.setEntityServiceClass(c.getEntityServiceClass());
                        iaf = AutowireHelper.getInstance().autowireAndProxy(iaf);
                        //TODO initialize throws IllegalStateException should we catch it and report it somewhere?
                        iaf.initialize();
                        ClientFriendlyImportAdapterConfig clientFriendlyImportAdapterConfig = new ClientFriendlyImportAdapterConfig();
                        clientFriendlyImportAdapterConfig.setAdapterName(c.getAdapterName());
                        clientFriendlyImportAdapterConfig.setParameters(c.getParameters());

                        if( c.getSupportedFileExtensions()!=null && !c.getSupportedFileExtensions().isEmpty()) {
                            log.trace("using extensions from config: {}", String.join("|", c.getSupportedFileExtensions()));
                            clientFriendlyImportAdapterConfig.setFileExtensions(c.getSupportedFileExtensions());
                        } else if( iaf.getSupportedFileExtensions()!=null && !iaf.getSupportedFileExtensions().isEmpty()){
                            log.trace("using extensions from importer: {}", String.join("|", iaf.getSupportedFileExtensions()));
                            clientFriendlyImportAdapterConfig.setFileExtensions(iaf.getSupportedFileExtensions());
                        }
                        clientFriendlyImportAdapterConfig.setAdapterKey( iaf.getAdapterKey());
                        clientFriendlyImportAdapterConfig.setDescription(c.getDescription());

                        return clientFriendlyImportAdapterConfig;
                    } catch (Exception e) {
                        log.warn("exception during import adapter factory init: ", e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized Class<T> getDefaultStagingAreaService(String context) {
        String clsName= gsrsFactoryConfiguration.getDefaultStagingAreaServiceClass().get(context);
        if(clsName==null || clsName.length()==0) {
            clsName="gsrs.stagingarea.service.DefaultStagingAreaService";
        }
        try {
            return (Class<T>) Class.forName(clsName);
        } catch (ClassNotFoundException e) {
            log.error("Class {} not found", clsName);
            throw new RuntimeException(e);
        }
    }



    @Override
    public Class<T> getDefaultStagingAreaEntityService(String context) {
        synchronized (ConfigBasedGsrsImportAdapterFactoryFactory.class) {
            return serviceMap.computeIfAbsent(context, (c) -> {
                log.trace("instantiating a staging area for context {}", context);
                String clsName = gsrsFactoryConfiguration.getDefaultStagingAreaEntityService().get(context);
                try {
                    return (Class<T>) Class.forName(clsName);
                } catch (ClassNotFoundException e) {
                    log.error("Class {} not found", clsName);
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Override
    public StagingAreaService getStagingAreaService(String context) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        //todo: cache instances of staging areas in hashmap
        log.trace("in getStagingAreaService for context {}", context);
        synchronized (ConfigBasedGsrsImportAdapterFactoryFactory.class) {
            return serviceInstanceMap.computeIfAbsent(context, (c) -> {
                Class<T> stagingAreaServiceClass = getDefaultStagingAreaService(context);
                try {
                    Constructor<?> constructor = stagingAreaServiceClass.getConstructor();
                    Object o = constructor.newInstance();
                    StagingAreaService service = AutowireHelper.getInstance().autowireAndProxy((StagingAreaService) o);
                    log.trace("instantiated service");
                    Class<?> stagingAreaEntityServiceClass = getDefaultStagingAreaEntityService(context);
                    log.trace("going to use entity service class: {}", stagingAreaEntityServiceClass.getName());
                    Constructor<?> constructorEntityService = stagingAreaEntityServiceClass.getConstructor();
                    Object o2 = constructorEntityService.newInstance();
                    log.trace("instantiated entity service");
                    StagingAreaEntityService entityService = AutowireHelper.getInstance().autowireAndProxy((StagingAreaEntityService<T>) o2);
                    service.registerEntityService(entityService);
                    log.trace("called registerEntityService with {}", entityService.getClass().getName());
                    return service;
                }
                catch (Exception ex){
                    log.error("Error instantiating staging area service", ex);
                }
                return null;
            });
        }

    }

    @Override
    public ProcessingAction<T> getMatchingProcessingAction(String context, String actionName) {
        log.trace("in getMatchingProcessingAction");
        if( processingActionClasses.isEmpty()) {
            log.trace("instantiating list");
            List<String> classNames= gsrsFactoryConfiguration.getAvailableProcessActions().get(context);
            classNames.forEach(className->{
                log.trace("class: {}", classNames);
                try {
                    Class<?> clazz = Class.forName(className);
                    Constructor<?> constructor =clazz.getConstructor();
                    ProcessingAction<T> action= (ProcessingAction<T>) constructor.newInstance();
                    AutowireHelper.getInstance().autowire(action);
                    processingActionClasses.add(action);
                } catch (Exception e) {
                    log.error("Class {} not found", className);
                    throw new RuntimeException(e);
                }
            });
        }
        for( ProcessingAction<T> action : processingActionClasses) {
            if( action.getActionName().equalsIgnoreCase(actionName)) {
                log.trace("found a match");
                return action;
            }
        }
        log.warn("found NO match for {}", actionName);
        return null;
    }

}
