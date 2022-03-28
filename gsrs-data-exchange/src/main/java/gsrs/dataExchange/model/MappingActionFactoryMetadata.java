package gsrs.dataExchange.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class MappingActionFactoryMetadata {
    private String label;
    private List<MappingParameter> parameterFields;

    public MappingActionFactoryMetadataBuilder builder() {
        return MappingActionFactoryMetadataBuilder.instance();
    }

    public MappingActionFactoryMetadata(MappingActionFactoryMetadataBuilder builder) {
        this.label = builder.getLabel();
        this.parameterFields = builder.getParameterFields();
    }

    public Map<String, Object> resolve(Map<String, Object> initialParams){
        Map<String,Object> returnMap = new LinkedHashMap<>();
        initialParams.forEach((k,v)->returnMap.put(k,v));
        for(MappingParameter param: parameterFields){
            if(!returnMap.containsKey(param.getFieldName())){
                Object o = param.getDefaultValue();
                if(o!=null){
                    returnMap.put(param.getFieldName(),o);
                }
                if(param.isRequired()){
                    throw new IllegalStateException("Required field:\"" +  param.getFieldName() + "\" is not present and has no default value");
                }
            }
        }
        return returnMap;
    }


}
