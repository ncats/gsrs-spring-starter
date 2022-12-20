package gsrs.imports;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import ix.core.util.InheritanceTypeIdResolver;
import ix.ginas.models.GinasCommonData;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@InheritanceTypeIdResolver.DefaultInstance
@AllArgsConstructor
@NoArgsConstructor
public class DefaultImportAdapterFactoryConfig implements ImportAdapterFactoryConfig {

    private Class importAdapterFactoryClass;
    //private Class newObjClass;
    //private List<ActionConfig> actions;
    private List<String> extensions;
    private String adapterName;
    private Map<String, Object> parameters;
    private Class holdingAreaServiceClass;
    private List<Class> entityServices;
    private Class entityServiceClass;

    private String description;

    public DefaultImportAdapterFactoryConfig(String adapterName, Class importAdapterFactoryClass, List<String> extensions) {
        this.adapterName=adapterName;
        this.importAdapterFactoryClass= importAdapterFactoryClass;
        this.extensions=extensions;
    }

    public DefaultImportAdapterFactoryConfig(String adapterName, String importAdapterFactoryClassName, List<String> extensions) {
        this.adapterName=adapterName;
        try {
            this.importAdapterFactoryClass= Class.forName( importAdapterFactoryClassName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        this.extensions=extensions;
    }
    /**
     * Catch all for additional JSON properties found will be assumed to be
     * parameters to pass to the validator class similar to {@link #parameters}.
     * if both parameters and unknown parameters are both set then parameters takes
     * precedence.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> unknownParameters = new ConcurrentHashMap<>();

    /**
     * Catch all for additional JSON properties found will be assumed to be
     * parameters to pass to the validator class similar to {@link #parameters}.
     * If both parameters and unknown parameters are both set then parameters takes
     * precedence.
     */
    @JsonAnySetter
    public void addUnknownParameter(String propertyKey, Object value){
        unknownParameters.put(propertyKey, value);
    }

    @Override
    public Class getImportAdapterFactoryClass() {
        return this.importAdapterFactoryClass;
    }

    @Override
    public void setImportAdapterFactoryClass(Class importAdapterFactoryClass) {
        this.importAdapterFactoryClass = importAdapterFactoryClass;
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Map<String, Object> params) {
        parameters=params;
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
    public ImportAdapterFactory<GinasCommonData> newImportAdapterFactory(ObjectMapper mapper, ClassLoader classLoader)  {

        if(parameters !=null && !parameters.isEmpty()){
            return (ImportAdapterFactory<GinasCommonData>) mapper.convertValue(parameters, importAdapterFactoryClass);
        }
        if(unknownParameters !=null && !unknownParameters.isEmpty()){
            return (ImportAdapterFactory<GinasCommonData>) mapper.convertValue(unknownParameters, importAdapterFactoryClass);

        }
        return (ImportAdapterFactory<GinasCommonData>) mapper.convertValue(Collections.emptyMap(), importAdapterFactoryClass);

    }

    @Override
    public Class getHoldingAreaServiceClass() {
        return this.holdingAreaServiceClass;
    }

    @Override
    public void setHoldingAreaServiceClass(Class clazz) {
        this.holdingAreaServiceClass = clazz;
    }

    @Override
    public List<Class> getEntityServices() {
        return this.entityServices;
    }

    @Override
    public void setEntityServices(List<Class> entityServices) {
        this.entityServices= entityServices;
    }

    @Override
    public Class getEntityServiceClass() {
        return entityServiceClass;
    }

    @Override
    public void setEntityServiceClass(Class newClass) {
        entityServiceClass= newClass;
    }

    @Override
    public String getDescription(){
        return this.description;
    }
    @Override
    public void setDescription(String description){
        this.description=description;
    }

}
