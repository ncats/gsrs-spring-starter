package ix.ginas.importers;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class InputFieldStatistics {

    private static final int MAX_READS=10;

    private final int maxExamples;

    private String field;
    private List<String> examples= new ArrayList<>();

    private Object valueRange;//for numerics

    private int count;

    private int distinctCount;

    public InputFieldStatistics(String f){
        this.field=f;
        this.maxExamples=MAX_READS;
    }

    public InputFieldStatistics(String f, int max){
        this.field=f;
        this.maxExamples=max;
    }
    public InputFieldStatistics add(String val){
        if(examples.size()<maxExamples){
            examples.add(val);
        }
        return this;
    }
/* Some ideas to help suggest best imports
TODO: consider other data types like:
1. Numeric
2. Numeric + Units (e.g. properties)
3. INCHI
4. INCHI-KEY
5. SMILES
6. CAS number
...

        private int minLines;
        private int maxLines;
        private int minLength;
        private int maxLength;

        private boolean allNumeric=true;
        private boolean allIntegers=true;
*/


}
