package gsrs.imports;

import gsrs.stagingarea.service.StagingAreaService;
import org.apache.poi.ss.formula.functions.T;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/*
Create an ImportAdapterFactory based on context (type of entity) and class of Adapter factory
 */
public interface GsrsImportAdapterFactoryFactory {

    <T> List<ImportAdapterFactory<T>> newFactory(String context, Class <T> clazz);

    List<String> getAvailableAdapterNames(String context);

    <T> List<ClientFriendlyImportAdapterConfig> getConfiguredAdapters(String context, Class <T> clazz);

    Class<T> getDefaultStagingAreaService(String context);

    Class<T> getDefaultStagingAreaEntityService(String context);

    StagingAreaService getStagingAreaService(String context) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException;
}
