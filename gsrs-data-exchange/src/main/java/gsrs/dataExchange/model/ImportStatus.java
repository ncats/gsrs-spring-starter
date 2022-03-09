package gsrs.dataExchange.model;

import lombok.Data;

@Data
public class ImportStatus {
    private String fileName;
    private String fileSize;
    private String adapterClass;
    private String adapterInitializerSchema; //pass around a String for now; punting on the structure of the schema
    private AdapterInitializer adapterInitializer;
    private String id;
    private String status;
}
