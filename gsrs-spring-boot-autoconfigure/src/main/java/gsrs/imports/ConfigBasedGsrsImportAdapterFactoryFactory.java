package gsrs.imports;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.GsrsFactoryConfiguration;
import gsrs.springUtils.AutowireHelper;
import gsrs.stagingarea.service.DefaultStagingAreaService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.formula.functions.T;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class ConfigBasedGsrsImportAdapterFactoryFactory implements GsrsImportAdapterFactoryFactory {

    @Autowired
    private GsrsFactoryConfiguration gsrsFactoryConfiguration;

    @Override
    public <T> List<ImportAdapterFactory<T>> newFactory(String context, Class <T> clazz) {
        log.trace("newFactory.  clazz: " + clazz.getName());
        List<? extends ImportAdapterFactoryConfig> configs = gsrsFactoryConfiguration.getImportAdapterFactories(context);
        ObjectMapper mapper = new ObjectMapper();
        return configs.stream().map(c ->
                {
                    try {
                        ImportAdapterFactory<T> iaf = (ImportAdapterFactory<T>) c.newImportAdapterFactory(mapper, AutowireHelper.getInstance().getClassLoader());
                        log.trace("c.getStagingServiceClass(): {}", c.getStagingAreaServiceClass()==null ? "null!!" :
                                c.getStagingAreaServiceClass().getName());
                        iaf.setStagingAreaService(c.getStagingAreaServiceClass());
                        log.trace("entity services:");
                        //c.getEntityServices().forEach(k->log.trace("k: {} ", k.getName()));
                        //iaf.setEntityServices(c.getEntityServices());
                        iaf.setEntityServiceClass(c.getEntityServiceClass());
                        iaf = AutowireHelper.getInstance().autowireAndProxy(iaf);

                        //allow config to override what's in code
                        if(c.getDescription() !=null && c.getDescription().length()>0) {
                            iaf.setDescription(c.getDescription());
                        }
                        log.trace("using description {} for this iaf", iaf.getDescription());
                        if(c.getSupportedFileExtensions()!=null && !c.getSupportedFileExtensions().isEmpty()) {
                            log.trace("passing on extensions from config: {}", String.join("***", c.getSupportedFileExtensions()));
                            iaf.setSupportedFileExtensions(c.getSupportedFileExtensions());
                        } else {
                            log.trace("using extensions within class");
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
        return configs.stream().map(c -> c.getAdapterName())
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
    public Class<T> getDefaultStagingAreaService(String context) {
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
        String clsName= gsrsFactoryConfiguration.getDefaultStagingAreaEntityService().get(context);
        try {
            return (Class<T>) Class.forName(clsName);
        } catch (ClassNotFoundException e) {
            log.error("Class {} not found", clsName);
            throw new RuntimeException(e);
        }
    }
}
