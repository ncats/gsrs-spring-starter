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
import org.springframework.http.MediaType;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for authentication through the ix.session cookie.
 */
@SpringJUnitConfig(SessionAuthenticationIntegrationTest.TestConfiguration.class)
@WebAppConfiguration
class SessionAuthenticationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private SessionRepository sessionRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void validSessionCookieAuthenticatesThroughTheFilterChain() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Session session = sessionFor("alice", sessionId);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        mockMvc.perform(get("/protected")
                        .cookie(new Cookie("ix.session", sessionId.toString())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("alice"));
    }

    @Test
    void expiredSessionCookieIsRejected() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Session session = sessionFor("alice", sessionId);
        session.expired = true;
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        mockMvc.perform(get("/protected")
                        .cookie(new Cookie("ix.session", sessionId.toString())))
                .andExpect(status().isForbidden());
    }

    @Test
    void unknownSessionCookieIsRejected() throws Exception {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/protected")
                        .cookie(new Cookie("ix.session", sessionId.toString())))
                .andExpect(status().isForbidden());
    }

    @Test
    void repeatedRequestsReuseTheSameSessionId() throws Exception {
        UUID sessionId = UUID.randomUUID();
        Session session = sessionFor("alice", sessionId);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        Cookie sessionCookie = new Cookie("ix.session", sessionId.toString());

        mockMvc.perform(get("/protected").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(content().string("alice"));
        mockMvc.perform(get("/protected").cookie(sessionCookie))
                .andExpect(status().isOk())
                .andExpect(content().string("alice"));

        verify(sessionRepository, times(2)).findById(sessionId);
    }

    private Session sessionFor(String username, UUID sessionId) {
        UserProfile profile = new UserProfile(new Principal(username, null));
        profile.active = true;
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
        SessionRepository sessionRepository() {
            return mock(SessionRepository.class);
        }

        @Bean
        UserProfileRepository userProfileRepository() {
            return mock(UserProfileRepository.class);
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
