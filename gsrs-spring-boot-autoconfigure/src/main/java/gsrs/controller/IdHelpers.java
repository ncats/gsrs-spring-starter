package gsrs.controller;

import java.util.regex.Pattern;

/**
 * Common implementations of {@link IdHelpers}.
 */
public enum IdHelpers implements IdHelper {
    /**
     * An ID that is a only digits.
     */
    NUMBER("[0-9]+", "\\D+"),
    STRING_NO_WHITESPACE("\\S+"),
    /**
     * An ID that is a UUID.
     */
    UUID( "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$",
            //I don't think lookaheads with curly braces are supported in Java 8?
            // so for now just make a a-z0-9 check that's not the exact length as uuid
//            "(?![a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9]-[a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9]-[a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9]-[a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9]-[a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9])"
//            "(?![a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})"
            "([a-zA-Z0-9\\-]{1,35})|([a-zA-Z0-9\\-]{37,})"
    ),
    CUSTOM(".*")
    ;

    private final String regexAsString;
    private final Pattern pattern;

    private final String notRegex;

    IdHelpers(String regex, String notRegex) {
        this.regexAsString = regex;
        this.notRegex = notRegex;
        pattern = Pattern.compile("^"+regexAsString+"$");
    }
    IdHelpers(String regex) {
        this(regex, ".+");
    }

    @Override
    public String getRegexAsString() {
        return regexAsString;
    }

    @Override
    public Pattern getPattern() {
        return pattern;
    }

    @Override
    public String getInverseRegexAsString() {
        return notRegex;
    }

    public String replaceId(String input, String idLiteral){
        return input.replace(idLiteral, regexAsString);
    }
    public String replaceInverseId(String input, String idLiteral){
        return input.replace(idLiteral, notRegex);
    }
}
