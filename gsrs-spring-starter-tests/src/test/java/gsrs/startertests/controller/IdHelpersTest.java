package gsrs.startertests.controller;

import gsrs.controller.IdHelpers;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;

//@Disabled("move this to starter tests and fix inverse")
public class IdHelpersTest {

    @Test
    public void number(){
        Pattern pattern = Pattern.compile(IdHelpers.NUMBER.getRegexAsString());
        assertThat("2", matchesPattern(pattern));
        assertThat("12345", matchesPattern(pattern));

        assertThat("123 45", not(matchesPattern(pattern)));
        assertThat("12345abc", not(matchesPattern(pattern)));
        assertThat("abc", not(matchesPattern(pattern)));
    }

    @Test
    @Disabled("this doesn't work as expected and will be removed soon")
    public void notANumber(){
        Pattern pattern = Pattern.compile(IdHelpers.NUMBER.getInverseRegexAsString());
        assertThat("2", not(matchesPattern(pattern)));
        assertThat("12345", not(matchesPattern(pattern)));

        assertThat("abc", matchesPattern(pattern));
        assertThat("12345abc", matchesPattern(pattern));
        assertThat("abc12345", matchesPattern(pattern));
        assertThat("abc12345abc", matchesPattern(pattern));
        assertThat("123sabc45abc", matchesPattern(pattern));
    }

    @Test
    public void uuid(){
        UUID uuid = UUID.randomUUID();
        String uuidWithDashes = uuid.toString();
        Pattern pattern = Pattern.compile(IdHelpers.UUID.getRegexAsString());

        assertThat(uuidWithDashes, matchesPattern(pattern));
        assertThat(uuidWithDashes.replace("-",""), not(matchesPattern(pattern)));
        assertThat("abc", not(matchesPattern(pattern)));
        assertThat("123", not(matchesPattern(pattern)));
        assertThat("fooBar", not(matchesPattern(pattern)));

        assertThat(uuidWithDashes.substring(15), not(matchesPattern(pattern)));
        assertThat(uuidWithDashes+"-"+uuidWithDashes, not(matchesPattern(pattern)));
    }
    @Test
    @Disabled("this doesn't work as expected and will be removed soon")
    public void notUuid(){
        UUID uuid = UUID.randomUUID();
        String uuidWithDashes = uuid.toString();
        Pattern pattern = Pattern.compile("^"+IdHelpers.UUID.getInverseRegexAsString()+"$");
        System.out.println(uuidWithDashes);

        assertThat(uuidWithDashes, not(matchesPattern(pattern)));
        assertThat(uuidWithDashes.replace("-",""), matchesPattern(pattern));
        assertThat("abc", matchesPattern(pattern));
        assertThat("123", matchesPattern(pattern));
        assertThat("fooBar", matchesPattern(pattern));

        assertThat(uuidWithDashes.substring(15), matchesPattern(pattern));
        assertThat(uuidWithDashes+"-"+uuidWithDashes, matchesPattern(pattern));
    }
}
