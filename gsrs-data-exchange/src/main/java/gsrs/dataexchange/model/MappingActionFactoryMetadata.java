package gsrs.dataexchange.model;

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

    public Map<String, Object> resolveAndValidate(Map<String, Object> initialParams){
        Map<String,Object> returnMap = new LinkedHashMap<>();
        returnMap.putAll(initialParams);
        for(MappingParameter param: parameterFields){
            if(returnMap.containsKey(param.getLookupKey())){
                System.out.printf("replaced %s (value: %s) with %s\n", param.getLookupKey(),
                        returnMap.get(param.getLookupKey()), param.getFieldName());
                /*returnMap.put(param.getFieldName(), returnMap.get(param.getLookupKey()));
                if?????????
                returnMap.remove(param.getLookupKey());*/
                //todo: validate type
            } else {
                Object o = param.getDefaultValue();
                if(o!=null){
                    returnMap.put(param.getFieldName(),o);
                } else if(param.isRequired()){
                    //todo: is a parameter that is REQUIRED AND has a default value but no user-supplied value an error? a realistic check?
                    throw new IllegalStateException("Required field:\"" +  param.getFieldName() + "\" is not present and has no default value");
                }
            }
        }
        return returnMap;
    }


}
