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
                        iaf.setHoldingAreaService(c.getHoldingServiceClass());
                        log.trace("c.getHoldingServiceClass(): {}", c.getHoldingServiceClass().getName());
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
}
