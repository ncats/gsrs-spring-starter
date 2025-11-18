package gsrs.imports.indexers;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.regex.Pattern;

@Data
@AllArgsConstructor
public class ValidationMessageSubstitution {
    private Pattern toMatch;
    private String substitution;

    public static ValidationMessageSubstitution of(String patternInfo, String substitutionInfo) {
        return new ValidationMessageSubstitution(Pattern.compile(patternInfo), substitutionInfo);
    }
}
