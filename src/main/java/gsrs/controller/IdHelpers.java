package gsrs.controller;

/**
 * Common implementations of {@link IdHelpers}.
 */
public enum IdHelpers implements IdHelper {
    /**
     * An ID that is a only digits.
     */
    NUMBER("[0-9]+", "^\\d*[a-zA-Z][a-zA-Z\\d]*"),
    STRING_NO_WHITESPACE("\\S+"),
    /**
     * An ID that is a UUID.
     */
    UUID( "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}",
            //I don't think lookaheads with curly braces are supported in Java 8?
            // so for now just make a a-z0-9 check that's not the exact length as uuid
//            "(?![a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9]-[a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9]-[a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9]-[a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9]-[a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9][a-fA-F0-9])"
//            "(?![a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12})"
            "([a-zA-Z0-9\\-]{1,35})|([a-zA-Z0-9\\-]{37,})"
    ),
    CUSTOM(".*")
    ;

    private final String regex;
    private final String notRegex;

    IdHelpers(String regex, String notRegex) {
        this.regex = regex;
        this.notRegex = notRegex;
    }
    IdHelpers(String regex) {
        this(regex, ".+");
    }

    @Override
    public String getRegexAsString() {
        return regex;
    }

    @Override
    public String getInverseRegexAsString() {
        return notRegex;
    }

    public String replaceId(String input, String idLiteral){
        return input.replace(idLiteral, regex);
    }
    public String replaceInverseId(String input, String idLiteral){
        return input.replace(idLiteral, notRegex);
    }
}
