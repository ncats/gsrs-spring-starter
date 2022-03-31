package ix.core.search.text;

import lombok.Data;

import java.util.regex.Pattern;

@Data
public class StandardEncoding implements StandardEncoder{

    private static String regex;
    private static String replaceWith;
    private static Pattern _pattern;

    public StandardEncoding() {}

    public StandardEncoding(String regex, String replaceWith) {
        this.regex = regex;
        this.replaceWith = replaceWith;
    }

    public static String encode(String s){
         if(_pattern==null){
            _pattern=Pattern.compile(Pattern.quote(regex));
         }
         s=_pattern.matcher(s).replaceAll(replaceWith);
         return s;
    }
}
