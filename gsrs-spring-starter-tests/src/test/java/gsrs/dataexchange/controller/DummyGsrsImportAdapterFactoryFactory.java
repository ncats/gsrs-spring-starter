package gsrs.dataexchange.controller;

import gsrs.dataexchange.model.ProcessingAction;
import gsrs.imports.ClientFriendlyImportAdapterConfig;
import gsrs.imports.GsrsImportAdapterFactoryFactory;
import gsrs.imports.ImportAdapterFactory;
import gsrs.stagingarea.service.StagingAreaService;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;

public class DummyGsrsImportAdapterFactoryFactory implements GsrsImportAdapterFactoryFactory {
    @Override
    public <T> List<ImportAdapterFactory<T>> newFactory(String context, Class<T> clazz) {
        ImportAdapterFactory<T> factory = (ImportAdapterFactory<T>) new DummyImportAdapterFactory();
        return Collections.singletonList(factory);
    }

    @Override
    public List<String> getAvailableAdapterNames(String context) {
        return Collections.singletonList(DummyImportAdapterFactory.ADAPTER_NAME);
    }

    @Override
    public <T> List<ClientFriendlyImportAdapterConfig> getConfiguredAdapters(String context, Class<T> clazz) {
        return null;
    }

    @Override
    public Class<?> getDefaultStagingAreaService(String context) {
        return null;
    }

    @Override
    public Class<?> getDefaultStagingAreaEntityService(String context) {
        return null;
    }

    @Override
    public StagingAreaService getStagingAreaService(String context) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        return null;
    }

    @Override
    public ProcessingAction<?> getMatchingProcessingAction(String context, String actionName) {
        return null;
    }
}
