package gsrs.importer;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/*
bare-bones implementation of 1 March 2023.
Intention: revisit and extend
 */
@Slf4j
@Data
public class ImportFieldStatistics {
    private String fieldType = "";
    private Object valueRange;//for numerics
    private List sampleValues = new ArrayList();
    private int count;
    private int distinctCount;

}
