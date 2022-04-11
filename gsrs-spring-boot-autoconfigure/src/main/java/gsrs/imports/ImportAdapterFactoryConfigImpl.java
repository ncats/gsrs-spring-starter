package gsrs.imports;

import java.util.List;
import java.util.Map;

public class ImportAdapterFactoryConfigImpl implements ImportAdapterFactoryConfig {

    private Class importAdapterFactoryClass;
    private Class newObjClass;
    private List<ActionConfig> actions;
    private List<String> extensions;
    private String adapterName;

    @Override
    public Class getImportAdapterFactoryClass() {
        return this.importAdapterFactoryClass;
    }

    @Override
    public void setImportAdapterFactoryClass(Class importAdapterFactoryClass) {
        this.importAdapterFactoryClass = importAdapterFactoryClass;
    }

    @Override
    public void setNewObjClass(Class newObjClass) {
        this.newObjClass = newObjClass;
    }

    @Override
    public Class getNewObjClass() {
        return this.newObjClass;
    }

    @Override
    public List<Map<String, Object>> getParameters() {
        return null;
    }

    @Override
    public String getAdapterName() {
        return this.adapterName;
    }

    @Override
    public void setAdapterName(String name) {
        this.adapterName=name;
    }

    @Override
    public List<String> getExtensions() {
        return this.extensions;
    }

    @Override
    public void setExtensions(List<String> extensions) {
        this.extensions=extensions;
    }

    @Override
    public List<ActionConfig> getActions() {
        return this.actions;
    }

    @Override
    public void setActions(List<ActionConfig> actions) {
        this.actions=actions;
    }
}
