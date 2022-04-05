package ix.core.search.text;

import lombok.Data;

import java.util.regex.Pattern;

@Data
public class StandardEncoding implements IndexedTextEncoder{

    private String regex;
    private String replaceWith;
    private Pattern _pattern;

    public StandardEncoding() {}

    public StandardEncoding(String regex, String replaceWith) {
        this.regex = regex;
        this.replaceWith = replaceWith;
        _pattern=Pattern.compile(Pattern.quote(regex));
    }

    @Override
    public String encode(String s){
        if(_pattern==null){
            _pattern=Pattern.compile(Pattern.quote(regex));
        }
        s=_pattern.matcher(s).replaceAll(replaceWith);
        return s;
    }
}