package gsrs.dataexchange.model;

import lombok.Data;

import java.util.Map;

@Data
public class ImportFieldHandling {
    private String fieldName;
    private String fieldOperation;
    private Map<String, String> fieldParameters;
}
