package ix.core.search.text;

import lombok.Data;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Data
public class ColonIndexedTextEncoder implements IndexedTextEncoder{

    private static final String COLON_WORD = "XCOLONX";

    public ColonIndexedTextEncoder() {}

    @Override
    public String encode(String s){
        return s.replaceAll(":", COLON_WORD);
    }

    @Override
    public String encodeQuery(String qtest){
        /*
        Quotes and colons have to handled with care when encoding the query search term.
        A colon may appear when specifying field names in the search. This colon should be
        conserved. But colons in the field value should be encoded. The problem is that
        Lucene indexing splits on punctuation such as the colon. That is why an encoder
        is needed. This is how a search having a colon should be encoded.
        Example:
        \"root_names_name:"AZT : ABC" AND root_names_name:"AZT : DEF"\"
        ==> \"root_names_name:"AZT XCOLONX ABC" AND root_names_name:"AZT XCOLONX DEF"\"
        */
        if(qtest.contains("\"") && qtest.contains(":")){
            // We want to conserve the escaped quotes that might be in the full search term.
            String tmp = qtest.replace("\\\"", "QUOTE_TEMPORARY");
            String[] parts = tmp.split("\"");
            for(int i=1;i<parts.length;i+=2){
                parts[i]="\""+parts[i].replace(":", COLON_WORD)+"\"";
            }
            return Arrays.stream(parts).collect(Collectors.joining(""))
            .replace("QUOTE_TEMPORARY","\\\"");
        }
        return qtest;
    }
}