package gsrs.imports;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.GsrsFactoryConfiguration;
import ix.ginas.models.GinasCommonData;
import org.checkerframework.checker.units.qual.Substance;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;

public class TestDefaultImportAdapterFactoryConfig {

    @Test
    public void testSetup() throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException, JsonProcessingException {
        DefaultImportAdapterFactoryConfig defaultImportAdapterFactoryConfig = new DefaultImportAdapterFactoryConfig();
        defaultImportAdapterFactoryConfig.setAdapterName("Blah");
        defaultImportAdapterFactoryConfig.setImportAdapterFactoryClass( Class.forName("gsrs.imports.DummyImportAdapterFactory"));
        defaultImportAdapterFactoryConfig.setExtensions(Arrays.asList(DummyImportAdapterFactory.extensions));
        GsrsFactoryConfiguration config = new GsrsFactoryConfiguration();
        Map<String, List<Map<String,Object>>> adapterConfig = new HashMap<>();
        List<Map<String,Object>> innerConfig = new ArrayList<>();
        adapterConfig.put("substance", innerConfig);

        config.setImportAdapterFactories(adapterConfig);

        ConfigBasedGsrsImportAdapterFactoryFactory factoryFactory = new ConfigBasedGsrsImportAdapterFactoryFactory();
        Field[] fields = factoryFactory.getClass().getDeclaredFields();
        for (Field field: fields
             ) {
            System.out.println(field.getName());
            if( field.getName().toUpperCase(Locale.ROOT).contains("CONFIG")) {
                field.setAccessible(true);
                field.set(factoryFactory, config);
                System.out.println("set field value");
            }
        }
        //Field configField= factoryFactory.getClass().getDeclaredField("gsrs.imports.ConfigBasedGsrsImportAdapterFactoryFactory.gsrsFactoryConfiguration");
        //configField.setAccessible(true);

        //configField.set(factoryFactory, defaultImportAdapterFactoryConfig);

        ObjectMapper om = new ObjectMapper();
        String config = om.writeValueAsString(defaultImportAdapterFactoryConfig);
        List<ImportAdapterFactory<GinasCommonData>> adapterFactories= factoryFactory.newFactory(config, GinasCommonData.class);
        Assertions.assertEquals(1, adapterFactories.size());
    }
}
