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
            // \"root_names_name:"AZT : ABC" AND root_names_name:"AZT : DEF"\"
            System.out.println("====> qtest:" + qtest);
            if(qtest.contains("\"") && qtest.contains(":")){
                String tmp = qtest.replace("\\\"", "QUOTE_TEMPORARY");
                String[] parts = tmp.split("\"");
                for(int i=1;i<parts.length;i+=2){
                    parts[i]="\""+parts[i].replace(":", "XCOLONX")+"\"";
                }
//            String modified1 = Arrays.stream(parts).collect(Collectors.joining("\""));
                String modified1 = Arrays.stream(parts).collect(Collectors.joining(""));
//            String modified1 = Arrays.stream(parts).map(p->"\""+p+"\"").collect(Collectors.joining(""));
                String modified2 = modified1.replace("QUOTE_TEMPORARY","\\\"");

                return modified2;
            }
            return qtest;
    }


}