package gsrs.imports;

import java.util.List;

public interface GsrsImportAdapterFactoryFactory {

    <T> List<ImportAdapterFactory<T>> newFactory(String context, Class <T> clazz);
}