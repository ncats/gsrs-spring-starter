package ix.ginas.exporters;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Data
public class ExporterSpecificExportSettings<T> {
    private List<String> columnNames;
    private boolean includeRepeatingDataOnEveryRow; //when multiple rows have the same values for a set of fields, do we need to repeat the values in the export file?
    private Function<T, T> transformation; // a function that takes an Object and returns a changed Object
    private Map<String, Object> parameters; //catch-all

}
