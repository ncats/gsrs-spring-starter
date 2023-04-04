package ix.ginas.importers;

import gsrs.importer.ImportFieldStatistics;
import lombok.Data;

import java.util.Map;

@Data
public class InputFileStatistics {
    private Map<String, ImportFieldStatistics> fieldData;

    private int recordCount;
}
