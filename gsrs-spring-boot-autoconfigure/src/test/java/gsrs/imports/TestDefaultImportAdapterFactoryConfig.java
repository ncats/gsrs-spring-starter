package gsrs.imports;

import gsrs.GsrsFactoryConfiguration;
import ix.ginas.models.GinasCommonData;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;

public class TestDefaultImportAdapterFactoryConfig {

    @Test
    @Disabled
    public void testSetup() throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException {
        String substanceContext="substances";
        GsrsFactoryConfiguration config = new GsrsFactoryConfiguration();
        Map<String, String> stagingAreaService = new HashMap<>();
        stagingAreaService.put("substances", "gsrs.stagingarea.service.DefaultStagingAreaService");
        config.setDefaultStagingAreaEntityService(stagingAreaService);

        Map<String, String> entityService = new HashMap<>();
        entityService.put("substances", "gsrs.stagingarea.service.StagingAreaEntityService");
        config.setDefaultStagingAreaEntityService(entityService);

        Map<String, List<Map<String,Object>>> adapterConfig = new HashMap<>();
        Map<String,Object> oneAdapter = new HashMap<>();
        oneAdapter.put("importAdapterFactoryClass", "gsrs.module.substance.importers.SDFImportAdaptorFactory");
        oneAdapter.put("adapterName", "NSRS SDF Adapter");
        oneAdapter.put("extensions", new String[] {"sdf", "sd"});
        oneAdapter.put("parameters", buildConfigParameters());
        oneAdapter.put("description", "general description");
        List<Map<String,Object>> adapters = new ArrayList<>();
        adapters.add(oneAdapter);
        adapterConfig.put(substanceContext, adapters);
        config.setImportAdapterFactories(adapterConfig);

        ConfigBasedGsrsImportAdapterFactoryFactory factoryFactory = new ConfigBasedGsrsImportAdapterFactoryFactory();
        Field[] fields = factoryFactory.getClass().getDeclaredFields();
        /*for (Field field: fields
             ) {
            System.out.println(field.getName());
            if( field.getName().toUpperCase(Locale.ROOT).contains("CONFIG")) {
                field.setAccessible(true);
                field.set(factoryFactory, config);
                System.out.println("set field value");
            }
        }  */
        Field configField= factoryFactory.getClass().getDeclaredField("gsrsFactoryConfiguration"); //gsrs.gsrs.startertests.imports.ConfigBasedGsrsImportAdapterFactoryFactory.
        configField.setAccessible(true);
        configField.set(factoryFactory, config);

        //configField.set(factoryFactory, defaultImportAdapterFactoryConfig);

        List<ImportAdapterFactory<GinasCommonData>> adapterFactories= factoryFactory.newFactory(substanceContext,
                GinasCommonData.class);
        Assertions.assertEquals(1, adapterFactories.size());
    }

    private Object buildConfigParameters(){
        Map<String, Object> parameters = new HashMap<>();
        List< Object> actions = new ArrayList<>();
        Map<String, Object> action1 = new HashMap<>();
        action1.put("actionName", "cas_import");
        action1.put("importActionFactoryClass", "gsrs.module.substance.importers.importActionFactories.NSRSCustomCodeExtractorActionFactory");

        List<Map<String, Object>> fields = new ArrayList<>();
        Map<String, Object> field1 = new HashMap<>();
        field1.put("fieldName", "CASNumber");
        field1.put("fieldLabel", "CAS Number");
        field1.put("fieldType", "java.lang.String");
        field1.put("required", true);
        field1.put("showInUi", true);
        fields.add(field1);
        Map<String, Object> field2 = new HashMap<>();
        field2.put("fieldName", "codeType");
        field2.put("fieldLabel", "Primary or Alternative");
        field2.put("fieldType", "java.lang.String");
        field2.put("required", false);
        field2.put("defaultValue", "PRIMARY");
        field2.put("showInUi", true);
        fields.add(field2);
        action1.put("fields", fields);

        actions.add(action1);
        parameters.put("actions", actions);
        return parameters;
    }
}
