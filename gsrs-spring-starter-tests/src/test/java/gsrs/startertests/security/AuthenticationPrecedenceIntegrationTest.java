package gsrs.startertests.security;

import gsrs.cache.GsrsCache;
import gsrs.repository.SessionRepository;
import gsrs.repository.UserProfileRepository;
import gsrs.security.GsrsAuthenticationValidator;
import gsrs.security.LegacyAuthenticationConfiguration;
import gsrs.security.LegacyAuthenticationFilter;
import gsrs.security.SessionConfiguration;
import gsrs.security.UserRoleConfiguration;
import gsrs.security.UserTokenCache;
import gsrs.services.PrivilegeService;
import ix.core.models.Principal;
import ix.core.models.Session;
import ix.core.models.UserProfile;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the credential precedence implemented by the legacy
 * authentication filter.
 */
@SpringJUnitConfig(AuthenticationPrecedenceIntegrationTest.TestConfiguration.class)
@WebAppConfiguration
class AuthenticationPrecedenceIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private UserTokenCache userTokenCache;

    @Autowired
    private PrivilegeService privilegeService;

    private MockMvc mockMvc;
    private UserProfile alice;
    private UserProfile bob;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        reset(userProfileRepository, sessionRepository, userTokenCache);

        alice = profile("alice");
        bob = profile("bob");
        when(privilegeService.getPrivilegesForRoles(any()))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void sessionAuthenticationTakesPrecedenceOverAnInvalidApiKey() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId))
                .thenReturn(Optional.of(sessionFor(alice, sessionId)));

        mockMvc.perform(get("/protected")
                        .cookie(new Cookie("ix.session", sessionId.toString()))
                        .header("auth-username", "alice")
                        .header("auth-key", "invalid-key"))
                .andExpect(status().isOk())
                .andExpect(content().string("alice"));
    }

    @Test
    void sessionAuthenticationTakesPrecedenceOverAComputedToken() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId))
                .thenReturn(Optional.of(sessionFor(alice, sessionId)));
        when(userTokenCache.getUserProfileFromToken("bob-token"))
                .thenReturn(bob);

        mockMvc.perform(get("/protected")
                        .cookie(new Cookie("ix.session", sessionId.toString()))
                        .header("auth-token", "bob-token"))
                .andExpect(status().isOk())
                .andExpect(content().string("alice"));
    }

    @Test
    void computedTokenTakesPrecedenceOverApiKey() throws Exception {
        when(userTokenCache.getUserProfileFromToken("bob-token"))
                .thenReturn(bob);
        when(userProfileRepository.findByUser_UsernameIgnoreCase("alice"))
                .thenReturn(alice);
        when(alice.acceptKey("alice-key")).thenReturn(true);

        mockMvc.perform(get("/protected")
                        .header("auth-token", "bob-token")
                        .header("auth-username", "alice")
                        .header("auth-key", "alice-key"))
                .andExpect(status().isOk())
                .andExpect(content().string("bob"));
    }

    @Test
    void expiredSessionFallsThroughToValidApiKey() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Session expiredSession = sessionFor(alice, sessionId);
        expiredSession.expired = true;
        when(sessionRepository.findById(sessionId))
                .thenReturn(Optional.of(expiredSession));
        when(userProfileRepository.findByUser_UsernameIgnoreCase("bob"))
                .thenReturn(bob);
        when(bob.acceptKey("bob-key")).thenReturn(true);

        mockMvc.perform(get("/protected")
                        .cookie(new Cookie("ix.session", sessionId.toString()))
                        .header("auth-username", "bob")
                        .header("auth-key", "bob-key"))
                .andExpect(status().isOk())
                .andExpect(content().string("bob"));
    }

    @Test
    void apiKeyRequiresAuthUsername() throws Exception {
        mockMvc.perform(get("/protected")
                        .header("auth-key", "alice-key"))
                .andExpect(status().isForbidden());
    }

    private UserProfile profile(String username) {
        UserProfile profile = mock(UserProfile.class);
        profile.user = new Principal(username, username + "@example.org");
        profile.active = true;
        when(profile.getRoles()).thenReturn(Collections.emptyList());
        when(profile.standardize()).thenReturn(profile);
        return profile;
    }

    private Session sessionFor(UserProfile profile, UUID sessionId) {
        Session session = new Session(profile);
        session.id = sessionId;
        return session;
    }

    @Configuration
    @EnableWebSecurity
    static class TestConfiguration {

        @Bean
        LegacyAuthenticationConfiguration authenticationConfiguration() {
            LegacyAuthenticationConfiguration configuration =
                    new LegacyAuthenticationConfiguration();
            configuration.setAllownonauthenticated(false);
            return configuration;
        }

        @Bean
        SessionConfiguration sessionConfiguration() {
            return new SessionConfiguration();
        }

        @Bean
        LegacyAuthenticationFilter legacyAuthenticationFilter() {
            return new LegacyAuthenticationFilter();
        }

        @Bean
        SecurityFilterChain securityFilterChain(
                HttpSecurity http,
                LegacyAuthenticationFilter authenticationFilter) throws Exception {
            return http
                    .addFilterBefore(authenticationFilter, LogoutFilter.class)
                    .authorizeHttpRequests(authorize -> authorize
                            .anyRequest().authenticated())
                    .csrf(csrf -> csrf.disable())
                    .exceptionHandling(exceptionHandling ->
                            exceptionHandling.authenticationEntryPoint(
                                    (request, response, exception) ->
                                            response.sendError(401)))
                    .build();
        }

        @Bean
        ProtectedController protectedController() {
            return new ProtectedController();
        }

        @Bean
        UserProfileRepository userProfileRepository() {
            return mock(UserProfileRepository.class);
        }

        @Bean
        SessionRepository sessionRepository() {
            return mock(SessionRepository.class);
        }

        @Bean
        UserTokenCache userTokenCache() {
            return mock(UserTokenCache.class);
        }

        @Bean
        GsrsCache gsrsCache() {
            return mock(GsrsCache.class);
        }

        @Bean
        PlatformTransactionManager platformTransactionManager() {
            return mock(PlatformTransactionManager.class);
        }

        @Bean
        UserRoleConfiguration userRoleConfiguration() {
            return new UserRoleConfiguration();
        }

        @Bean(name = "permission")
        PrivilegeService privilegeService() {
            PrivilegeService service = mock(PrivilegeService.class);
            when(service.getPrivilegesForRoles(any())).thenReturn(Collections.emptyList());
            return service;
        }

        @Bean
        GsrsAuthenticationValidator gsrsAuthenticationValidator() {
            return mock(GsrsAuthenticationValidator.class);
        }
    }

    @RestController
    static class ProtectedController {

        @GetMapping("/protected")
        String protectedEndpoint() {
            return org.springframework.security.core.context.SecurityContextHolder
                    .getContext().getAuthentication().getName();
        }
    }
}
