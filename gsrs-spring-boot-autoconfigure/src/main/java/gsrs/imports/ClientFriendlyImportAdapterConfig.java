package gsrs.imports;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Data
@Slf4j
public class ClientFriendlyImportAdapterConfig {

    private String adapterName;
    private String adapterKey;
    private List<String> fileExtensions;
    private Map<String, Object> parameters;
}
