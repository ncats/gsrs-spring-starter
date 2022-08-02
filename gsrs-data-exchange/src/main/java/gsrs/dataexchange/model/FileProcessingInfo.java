package gsrs.dataexchange.model;

import lombok.Data;

import java.nio.charset.StandardCharsets;

@Data
public class FileProcessingInfo {
    private String lineDelimiter;
    private String fieldDelimiter;
    private StandardCharsets charSet;
}
