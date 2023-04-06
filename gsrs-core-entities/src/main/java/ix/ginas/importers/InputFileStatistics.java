package ix.ginas.importers;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InputFileStatistics {
    private Map<String, InputFieldStatistics> fieldData;

    private int recordCount;
}
