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
import ix.core.models.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for authentication followed by method-level authorization.
 */
@SpringJUnitConfig(AuthorizationIntegrationTest.TestConfiguration.class)
@WebAppConfiguration
class AuthorizationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private PrivilegeService privilegeService;

    private MockMvc mockMvc;
    private UserProfile profile;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        profile = mock(UserProfile.class);
        profile.user = new Principal("alice", "alice@example.org");
        profile.active = true;
        when(profile.getRoles()).thenReturn(Collections.emptyList());
        when(profile.standardize()).thenReturn(profile);
        when(profile.acceptKey("api-key")).thenReturn(true);
        when(userProfileRepository.findByUser_UsernameIgnoreCase("alice"))
                .thenReturn(profile);
    }

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/admin-only"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedUserWithoutRequiredAuthorityIsForbidden() throws Exception {
        when(privilegeService.getPrivilegesForRoles(any())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/admin-only")
                        .header("auth-username", "alice")
                        .header("auth-key", "api-key"))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedUserWithRequiredAuthorityCanAccessEndpoint() throws Exception {
        when(privilegeService.getPrivilegesForRoles(any()))
                .thenReturn(List.of("Admin"));

        mockMvc.perform(get("/admin-only")
                        .header("auth-username", "alice")
                        .header("auth-key", "api-key"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("admin content"));
    }

    @Configuration
    @EnableWebSecurity
    @EnableMethodSecurity
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
        UserTokenCache userTokenCache() {
            return mock(UserTokenCache.class);
        }

        @Bean
        SessionRepository sessionRepository() {
            return mock(SessionRepository.class);
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

        @GetMapping("/admin-only")
        @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('Admin')")
        String adminOnlyEndpoint() {
            return "admin content";
        }
    }
}
