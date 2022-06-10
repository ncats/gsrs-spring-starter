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
        The colon would appear when specifying field names. We want to conserve this colon.
        But colons that in the term to be search on we want to encode before searching.
        Example:
        \"root_names_name:"AZT : ABC" AND root_names_name:"AZT : DEF"\"
        ==> \"root_names_name:"AZT XCOLONX ABC" AND root_names_name:"AZT XCOLONX DEF"\"
        */
        if(qtest.contains("\"") && qtest.contains(":")){
            // We want to conserve the escaped quotes that might in the full search term.
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