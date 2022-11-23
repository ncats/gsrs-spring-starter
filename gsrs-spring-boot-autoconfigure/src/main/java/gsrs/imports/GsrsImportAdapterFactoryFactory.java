package gsrs.imports;

import java.util.List;

/*
Create an ImportAdapterFactory based on context (type of entity) and class of Adapter factory
 */
public interface GsrsImportAdapterFactoryFactory {

    <T> List<ImportAdapterFactory<T>> newFactory(String context, Class <T> clazz);

    List<String> getAvailableAdapterNames(String context);

    <T> List<ClientFriendlyImportAdapterConfig> getConfiguredAdapters(String context, Class <T> clazz);
}