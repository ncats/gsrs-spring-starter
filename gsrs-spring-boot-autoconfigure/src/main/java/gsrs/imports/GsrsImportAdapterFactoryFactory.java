package gsrs.imports;

import java.util.List;

/*
Create an ImportAdapterFactory based on context (type of entity) and class of Adapter factory
 */
public interface GsrsImportAdapterFactoryFactory {

    <T> List<ImportAdapterFactory<T>> newFactory(String context, Class <T> clazz);
}