package ix.core.search.text;

import lombok.Data;

import java.util.regex.Pattern;

@Data
public class SpecialEncoding implements SpecialEncoder{

    private String regex;
    private String replaceWith;
    private Pattern _pattern;

    public SpecialEncoding() {}

    public SpecialEncoding(String regex, String replaceWith) {
        setRegex((regex));
        setReplaceWith(replaceWith);
    }

    public String encode(String s){
         if(_pattern==null){
            _pattern=Pattern.compile(Pattern.quote(regex));
         }
         s=_pattern.matcher(s).replaceAll(replaceWith);
         return s;
    }
}
