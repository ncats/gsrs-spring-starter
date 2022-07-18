package gsrs.holdingArea.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateRecordParameters {
    private String jsonData;
    private String source;
    private byte[] rawData;
    private InputStream rawDataSource;
    private Class entityClass;
    private String formatType;
}
