package gsrs.holdingarea.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ImportRecordParameters {
    private String jsonData;
    private String source;
    private byte[] rawData;
    private InputStream rawDataSource;
    private String entityClassName;
    private String formatType;
    private UUID recordId;//will be null for creation; populated when updating
    private String adapterName;
}
