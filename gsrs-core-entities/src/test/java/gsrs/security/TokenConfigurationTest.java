package gsrs.security;

import ix.utils.Util;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TokenConfigurationTest {

    @Test
    void returnsNullWhenApiKeyIsNull() {
        TokenConfiguration configuration = new TokenConfiguration();
        assertNull(configuration.getComputedToken("alice", null));
    }

    @Test
    void producesTheSameTokenForTheSameInputsWithinATimeBucket() {
        TokenConfiguration configuration = new TokenConfiguration();
        String first = configuration.getComputedToken("alice", "secret");
        String second = configuration.getComputedToken("alice", "secret");
        assertEquals(first, second);
    }

    @Test
    void changesWhenUsernameOrApiKeyChanges() {
        TokenConfiguration configuration = new TokenConfiguration();
        String token = configuration.getComputedToken("alice", "secret");
        assertNotEquals(token, configuration.getComputedToken("bob", "secret"));
        assertNotEquals(token, configuration.getComputedToken("alice", "other-secret"));
    }

    @Test
    void usesConfiguredCanonicalTimestampInTokenInput() {
        TokenConfiguration configuration = new TokenConfiguration();
        configuration.setTimeResolutionMS(Long.MAX_VALUE);
        assertEquals(0L, configuration.getCanonicalCacheTimeStamp());
        assertEquals(Util.sha1("0alicesecret"),
                configuration.getComputedToken("alice", "secret"));
    }
}
