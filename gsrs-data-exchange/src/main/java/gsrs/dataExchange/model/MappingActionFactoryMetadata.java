package gsrs.dataExchange.model;

import lombok.Data;

import java.util.List;

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

}
