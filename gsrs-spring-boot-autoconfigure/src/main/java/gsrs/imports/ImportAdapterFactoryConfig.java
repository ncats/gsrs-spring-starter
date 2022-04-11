package gsrs.imports;


import java.util.List;
import java.util.Map;

public interface ImportAdapterFactoryConfig {

    Class getImportAdapterFactoryClass();
    void setImportAdapterFactoryClass(Class importAdapterFactoryClass);

    void setNewObjClass(Class newObjClass);
    Class getNewObjClass();

    List<Map<String, Object>> getParameters();

    String getAdapterName();
    void setAdapterName(String name);

    List<String> getExtensions();
    void setExtensions(List<String> extensions);

    List<ActionConfig> getActions();
    void setActions(List<ActionConfig> actions);
}
