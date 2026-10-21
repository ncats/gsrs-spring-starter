package gsrs.startertests.security;

import gsrs.repository.UserProfileRepository;
import gsrs.security.TokenConfiguration;
import ix.core.models.Principal;
import ix.core.models.UserProfile;
import ix.ncats.controllers.auth.LegacyUserTokenCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for the Spring-managed legacy token cache. The repository
 * is mocked so the test exercises the real cache, token configuration, and
 * cache-to-repository lookup behavior together.
 */
@SpringJUnitConfig(TokenCacheIntegrationTest.TestConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TokenCacheIntegrationTest {

    @Autowired
    private LegacyUserTokenCache tokenCache;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private TokenConfiguration tokenConfiguration;

    @BeforeEach
    void setUp() {
        when(userProfileRepository.streamAllTokenInfo()).thenReturn(Stream.empty());
    }

    @Test
    void firstLookupLoadsProfileAndLaterLookupUsesTheCache() {
        UserProfileRepository.UserTokenInfo tokenInfo = tokenInfo("alice", "api-key");
        UserProfile profile = new UserProfile(new Principal("alice"));

        when(userProfileRepository.streamAllTokenInfo()).thenReturn(Stream.of(tokenInfo));
        when(userProfileRepository.findByUser_UsernameIgnoreCase("alice"))
                .thenReturn(profile);

        String token = tokenConfiguration.getComputedToken("alice", "api-key");

        assertSame(profile, tokenCache.getUserProfileFromToken(token));
        assertSame(profile, tokenCache.getUserProfileFromToken(token));
        verify(userProfileRepository).findByUser_UsernameIgnoreCase("alice");
    }

    @Test
    void unknownTokenReturnsNull() {
        assertNull(tokenCache.getUserProfileFromToken("unknown-token"));
    }

    @Test
    void updatingAUserMakesTheNewTokenImmediatelyAvailable() {
        UserProfile profile = cachedProfile("alice", "new-token");

        tokenCache.updateUserCache(profile);

        assertSame(profile, tokenCache.getUserProfileFromToken("new-token"));
    }

    @Test
    void evictingAUserRemovesTheTokenAndProfileCacheEntries() {
        UserProfile profile = cachedProfile("alice", "profile-token");

        tokenCache.updateUserCache(profile);
        assertSame(profile, tokenCache.getUserProfileFromToken("profile-token"));

        tokenCache.evictUser(profile);

        assertNull(tokenCache.getUserProfileFromToken("profile-token"));
    }

    @Test
    void computeUserIfAbsentCallsFetcherOnlyOnce() {
        UserProfile profile = cachedProfile("alice", "profile-token");
        AtomicInteger fetchCount = new AtomicInteger();

        assertSame(profile, tokenCache.computeUserIfAbsent("alice", username -> {
            fetchCount.incrementAndGet();
            return profile;
        }));
        assertSame(profile, tokenCache.computeUserIfAbsent("alice", username -> {
            fetchCount.incrementAndGet();
            return profile;
        }));

        assertEquals(1, fetchCount.get());
    }

    private UserProfile cachedProfile(String username, String token) {
        UserProfile profile = mock(UserProfile.class);
        Principal principal = mock(Principal.class);
        principal.username = username;
        profile.user = principal;
        when(profile.getRoles()).thenReturn(Collections.emptyList());
        when(profile.getComputedToken()).thenReturn(token);
        when(principal.computeStandardizedName()).thenReturn(username);
        return profile;
    }

    private UserProfileRepository.UserTokenInfo tokenInfo(String username, String key) {
        UserProfileRepository.UserTokenInfo tokenInfo =
                mock(UserProfileRepository.UserTokenInfo.class);
        when(tokenInfo.getUsername()).thenReturn(username);
        when(tokenInfo.getKey()).thenReturn(key);
        return tokenInfo;
    }

    @Configuration
    static class TestConfiguration {

        @Bean
        UserProfileRepository userProfileRepository() {
            return mock(UserProfileRepository.class);
        }

        @Bean
        TokenConfiguration tokenConfiguration() {
            TokenConfiguration configuration = new TokenConfiguration();
            // Keep the Ehcache TTL and canonical bucket stable during the test.
            configuration.setTimeResolutionMS(1_000_000_000_000L);
            return configuration;
        }

        @Bean
        LegacyUserTokenCache legacyUserTokenCache(
                UserProfileRepository repository,
                TokenConfiguration configuration) {
            return new LegacyUserTokenCache(repository, configuration);
        }
    }
}
