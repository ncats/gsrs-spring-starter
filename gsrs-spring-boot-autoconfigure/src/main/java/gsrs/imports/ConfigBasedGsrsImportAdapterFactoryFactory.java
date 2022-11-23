package gsrs.imports;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.GsrsFactoryConfiguration;
import gsrs.springUtils.AutowireHelper;
import lombok.extern.slf4j.Slf4j;
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
                        log.trace("c.getHoldingServiceClass(): {}", c.getHoldingAreaServiceClass()==null ? "null!!" :
                                c.getHoldingAreaServiceClass().getName());
                        iaf.setHoldingAreaService(c.getHoldingAreaServiceClass());
                        log.trace("entity services:");
                        //c.getEntityServices().forEach(k->log.trace("k: {} ", k.getName()));
                        //iaf.setEntityServices(c.getEntityServices());
                        iaf.setEntityServiceClass(c.getEntityServiceClass());
                        iaf = AutowireHelper.getInstance().autowireAndProxy(iaf);
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
                        log.trace("c.getHoldingServiceClass(): {}", c.getHoldingAreaServiceClass()==null ? "null!!" :
                                c.getHoldingAreaServiceClass().getName());
                        iaf.setHoldingAreaService(c.getHoldingAreaServiceClass());
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
                        clientFriendlyImportAdapterConfig.setFileExtensions(iaf.getSupportedFileExtensions());
                        clientFriendlyImportAdapterConfig.setAdapterKey( iaf.getAdapterKey());

                        return clientFriendlyImportAdapterConfig;
                    } catch (Exception e) {
                        log.warn("exception during import adapter factory init: ", e);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
