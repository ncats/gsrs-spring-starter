package gsrs.imports;

import org.apache.poi.ss.formula.functions.T;

import java.util.List;

/*
Create an ImportAdapterFactory based on context (type of entity) and class of Adapter factory
 */
public interface GsrsImportAdapterFactoryFactory {

    <T> List<ImportAdapterFactory<T>> newFactory(String context, Class <T> clazz);

    List<String> getAvailableAdapterNames(String context);

    <T> List<ClientFriendlyImportAdapterConfig> getConfiguredAdapters(String context, Class <T> clazz);

    Class<T> getDefaultHoldingAreaService(String context);

    Class<T> getDefaultHoldingAreaEntityService(String context);
}