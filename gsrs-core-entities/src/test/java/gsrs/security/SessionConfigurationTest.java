package gsrs.security;

import ix.core.models.Principal;
import ix.core.models.Session;
import ix.core.models.UserProfile;
import gsrs.repository.SessionRepository;
import gsrs.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SessionConfigurationTest {

    private SessionRepository sessionRepository;
    private UserProfileRepository userProfileRepository;
    private SessionConfiguration configuration;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(SessionRepository.class);
        userProfileRepository = mock(UserProfileRepository.class);
        configuration = new SessionConfiguration();
        configuration.setSessionRepository(sessionRepository);
        configuration.setUserProfileRepository(userProfileRepository);
    }

    @Test
    void activeSessionIsNotExpiredWithDefaultConfiguration() {
        assertFalse(configuration.isExpired(new Session()));
    }

    @Test
    void explicitlyExpiredSessionIsExpired() {
        Session session = new Session();
        session.expired = true;

        assertTrue(configuration.isExpired(session));
    }

    @Test
    void oldSessionIsExpiredWhenPositiveDurationHasElapsed() throws InterruptedException {
        Session session = new Session();
        configuration.setSessionExpirationMS(1L);

        Thread.sleep(5L);

        assertTrue(configuration.isExpired(session));
    }

    @Test
    void nullUserReturnsNoSessionAndDoesNotCallRepositories() {
        assertTrue(configuration.cleanUpSessionsThenGetSession(null).isEmpty());

        verifyNoInteractions(sessionRepository, userProfileRepository);
    }

    @Test
    void activeSessionIsReusedAndAccessTimeIsUpdated() {
        UserProfile userProfile = userProfile("alice");
        Session session = new Session(userProfile);
        long previousAccessTime = session.accessed;
        when(sessionRepository.getActiveSessionsFor(userProfile))
                .thenReturn(Collections.singletonList(session));

        Optional<Session> result = configuration.cleanUpSessionsThenGetSession(userProfile);

        assertTrue(result.isPresent());
        assertSame(session, result.get());
        assertTrue(session.accessed >= previousAccessTime);
        verify(sessionRepository).saveAll(any());
    }

    @Test
    void expiredSessionsAreSavedAndReplacedWhenNoActiveSessionRemains() {
        UserProfile userProfile = userProfile("alice");
        Session expiredSession = new Session(userProfile);
        expiredSession.expired = true;
        UserProfile refreshedProfile = userProfile("alice");
        when(sessionRepository.getActiveSessionsFor(userProfile))
                .thenReturn(List.of(expiredSession));
        when(userProfileRepository.findByUser_UsernameIgnoreCase("ALICE"))
                .thenReturn(refreshedProfile);

        Optional<Session> result = configuration.cleanUpSessionsThenGetSession(userProfile);

        assertTrue(result.isPresent());
        assertNotNull(result.get());
        assertSame(refreshedProfile, result.get().profile);
        assertTrue(expiredSession.expired);
        verify(sessionRepository).saveAndFlush(expiredSession);
        verify(sessionRepository).saveAndFlush(result.get());
    }

    private UserProfile userProfile(String username) {
        return new UserProfile(Principal.createStandardized(username, username + "@example.org"));
    }
}
