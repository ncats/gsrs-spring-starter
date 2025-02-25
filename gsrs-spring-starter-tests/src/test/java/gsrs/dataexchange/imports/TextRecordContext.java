package gsrs.dataexchange.imports;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class TextRecordContext {
    private String item1;
    private String item2;

    public TextRecordContext(String inputLine, String delim) {
        if(inputLine!=null && inputLine.length() >0 && inputLine.contains(delim)) {
            String[] tokens = inputLine.split(delim);
            this.item1 = tokens[0];
            this.item2 = tokens[1];
            log.trace("assigned data");
        }
    }
}
