package gsrs.importer;

import ix.ginas.importers.InputFieldStatistics;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImportFieldMetadata {
    private String fieldName;
    private InputFieldStatistics statistics;
}
