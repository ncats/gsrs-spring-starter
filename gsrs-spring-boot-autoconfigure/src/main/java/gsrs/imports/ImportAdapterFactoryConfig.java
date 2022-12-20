package gsrs.imports;


import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import ix.core.util.InheritanceTypeIdResolver;

import java.util.List;
import java.util.Map;

/*
Information necessary to create an ImportAdapterFactory
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CUSTOM, include = JsonTypeInfo.As.PROPERTY, property = "configClass", defaultImpl = DefaultImportAdapterFactoryConfig.class)
@JsonTypeIdResolver(InheritanceTypeIdResolver.class)
public interface ImportAdapterFactoryConfig {

    Class getImportAdapterFactoryClass();

    void setImportAdapterFactoryClass(Class importAdapterFactoryClass);

    Map<String, Object> getParameters();
    void setParameters(Map<String, Object> params);

    //optional: class may define its own name
    String getAdapterName();
    void setAdapterName(String name);

    //optional: class may set its own extensions
    List<String> getExtensions();
    void setExtensions(List<String> extensions);

    ImportAdapterFactory newImportAdapterFactory(ObjectMapper mapper, ClassLoader classLoader) throws ClassNotFoundException;

    Class getHoldingAreaServiceClass();
    void setHoldingAreaServiceClass(Class holdingServiceClass);

    List<Class> getEntityServices();
    void setEntityServices(List<Class> entityServices);

    Class getEntityServiceClass();
    void setEntityServiceClass(Class newClass);

    String getDescription();

    void setDescription(String description);

}
