package ix.ncats.controllers.auth;

import gsrs.repository.UserProfileRepository;
import gsrs.security.TokenConfiguration;
import ix.core.models.Principal;
import ix.core.models.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegacyUserTokenCacheTest {

    private UserProfileRepository repository;
    private TokenConfiguration tokenConfiguration;
    private LegacyUserTokenCache cache;

    @BeforeEach
    void setUp() {
        repository = mock(UserProfileRepository.class);
        tokenConfiguration = new TokenConfiguration();
        // Keep the test in one canonical time bucket and avoid a short Ehcache TTL.
        tokenConfiguration.setTimeResolutionMS(1_000_000_000_000L);
        cache = new LegacyUserTokenCache(repository, tokenConfiguration);
    }

    @Test
    void resolvesAValidTokenAndCachesTheLoadedProfile() {
        UserProfileRepository.UserTokenInfo tokenInfo = tokenInfo("alice", "api-key");
        UserProfile profile = new UserProfile(new Principal("alice"));

        when(repository.streamAllTokenInfo()).thenReturn(Stream.of(tokenInfo));
        when(repository.findByUser_UsernameIgnoreCase("alice")).thenReturn(profile);

        String token = tokenConfiguration.getComputedToken("alice", "api-key");

        assertSame(profile, cache.getUserProfileFromToken(token));
        assertSame(profile, cache.getUserProfileFromToken(token));
        verify(repository).findByUser_UsernameIgnoreCase("alice");
    }

    @Test
    void returnsNullForAnUnknownToken() {
        when(repository.streamAllTokenInfo()).thenReturn(Stream.empty());

        assertNull(cache.getUserProfileFromToken("not-a-valid-token"));
        verify(repository).streamAllTokenInfo();
    }

    @Test
    void evictUserRemovesItsTokenAndProfileEntries() {
        UserProfile profile = mock(UserProfile.class);
        Principal principal = mock(Principal.class);
        profile.user = principal;
        principal.username = "alice";
        when(principal.computeStandardizedName()).thenReturn("alice");
        when(profile.getComputedToken()).thenReturn("profile-token");
        cache.updateUserCache(profile);
        String token = "profile-token";

        assertSame(profile, cache.getUserProfileFromToken(token));

        cache.evictUser(profile);

        assertNull(cache.getUserProfileFromToken(token));
    }

    @Test
    void computeUserIfAbsentCachesTheFetchedProfile() {
        UserProfile profile = mock(UserProfile.class);
        Principal principal = mock(Principal.class);
        profile.user = principal;
        principal.username = "alice";
        when(profile.getComputedToken()).thenReturn("profile-token");
        AtomicInteger fetchCount = new AtomicInteger();

        assertSame(profile, cache.computeUserIfAbsent("alice", username -> {
            fetchCount.incrementAndGet();
            return profile;
        }));
        assertSame(profile, cache.computeUserIfAbsent("alice", username -> {
            fetchCount.incrementAndGet();
            return profile;
        }));

        assertEquals(1, fetchCount.get());
    }

    private UserProfileRepository.UserTokenInfo tokenInfo(String username, String key) {
        UserProfileRepository.UserTokenInfo tokenInfo = mock(UserProfileRepository.UserTokenInfo.class);
        when(tokenInfo.getUsername()).thenReturn(username);
        when(tokenInfo.getKey()).thenReturn(key);
        return tokenInfo;
    }
}
