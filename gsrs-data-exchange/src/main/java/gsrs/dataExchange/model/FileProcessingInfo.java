package gsrs.dataExchange.model;

import lombok.Data;

import javax.sound.sampled.AudioFormat;
import java.nio.charset.StandardCharsets;

@Data
public class FileProcessingInfo {
    private String lineDelimiter;
    private String fieldDelimiter;
    private StandardCharsets charSet;
}
