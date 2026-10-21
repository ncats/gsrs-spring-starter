package gsrs.security;

import gsrs.cache.GsrsCache;
import gsrs.repository.SessionRepository;
import gsrs.repository.UserProfileRepository;
import gsrs.services.PrivilegeService;
import gsrs.model.UserProfileAuthenticationResult;
import ix.core.models.Principal;
import ix.core.models.UserProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockedStatic;

class LegacyAuthenticationFilterTest {

    private LegacyAuthenticationFilter filter;
    private LegacyAuthenticationConfiguration authenticationConfiguration;
    private UserProfileRepository repository;
    private UserTokenCache userTokenCache;
    private PrivilegeService privilegeService;
    private GsrsAuthenticationValidator authenticationValidator;

    @BeforeEach
    void setUp() {
        filter = new LegacyAuthenticationFilter();
        authenticationConfiguration = new LegacyAuthenticationConfiguration();
        authenticationConfiguration.setTrustheader(false);
        authenticationConfiguration.setAllownonauthenticated(false);

        repository = mock(UserProfileRepository.class);
        userTokenCache = mock(UserTokenCache.class);
        privilegeService = mock(PrivilegeService.class);
        authenticationValidator = mock(GsrsAuthenticationValidator.class);

        SessionConfiguration sessionConfiguration = new SessionConfiguration();
        ReflectionTestUtils.setField(filter, "sessionConfiguration", sessionConfiguration);
        ReflectionTestUtils.setField(filter, "authenticationConfiguration", authenticationConfiguration);
        ReflectionTestUtils.setField(filter, "repository", repository);
        ReflectionTestUtils.setField(filter, "userTokenCache", userTokenCache);
        ReflectionTestUtils.setField(filter, "sessionRepository", mock(SessionRepository.class));
        ReflectionTestUtils.setField(filter, "gsrsCache", mock(GsrsCache.class));
        ReflectionTestUtils.setField(filter, "platformTransactionManager", mock(PlatformTransactionManager.class));
        ReflectionTestUtils.setField(filter, "privilegeService", privilegeService);
        ReflectionTestUtils.setField(filter, "gsrsAuthenticationValidator", authenticationValidator);

        when(privilegeService.getPrivilegesForRoles(anyList())).thenReturn(Collections.emptyList());
    }

    @Test
    void authenticatesWithComputedToken() throws Exception {
        UserProfile profile = activeProfile("alice");
        when(userTokenCache.getUserProfileFromToken("computed-token")).thenReturn(profile);

        MockHttpServletRequest request = requestWithHeader("auth-token", "computed-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertInstanceOf(LegacyUserTokenAuthentication.class,
                SecurityContextHolder.getContext().getAuthentication());
        assertEquals("ALICE", request.getSession().getAttribute("username"));
        verify(authenticationValidator).markAuthenticated();
        assertTrue(chain.getRequest() != null);
        clearSecurityContext();
    }

    @Test
    void authenticatesWithApiKey() throws Exception {
        UserProfile profile = activeProfile("alice");
        when(repository.findByUser_UsernameIgnoreCase("alice")).thenReturn(profile);
        when(profile.acceptKey("api-key")).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("auth-username", "alice");
        request.addHeader("auth-key", "api-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertInstanceOf(LegacyUserKeyAuthentication.class,
                SecurityContextHolder.getContext().getAuthentication());
        verify(repository).findByUser_UsernameIgnoreCase("alice");
        verify(authenticationValidator).markAuthenticated();
        clearSecurityContext();
    }

    @Test
    void authenticatesWithPassword() throws Exception {
        UserProfile profile = activeProfile("alice");
        when(repository.findByUser_UsernameIgnoreCase("alice")).thenReturn(profile);
        when(profile.acceptPassword("password"))
                .thenReturn(new UserProfileAuthenticationResult(true, false));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("auth-username", "alice");
        request.addHeader("auth-password", "password");
        MockHttpServletResponse response = new MockHttpServletResponse();

        try (MockedStatic<PrivilegeService> mockedPrivilegeService = mockStatic(PrivilegeService.class)) {
            mockedPrivilegeService.when(PrivilegeService::instance).thenReturn(privilegeService);
            filter.doFilter(request, response, new MockFilterChain());
        }

        assertInstanceOf(UserProfilePasswordAuthentication.class,
                SecurityContextHolder.getContext().getAuthentication());
        verify(authenticationValidator).markAuthenticated();
        clearSecurityContext();
    }

    @Test
    void computedTokenTakesPrecedenceOverApiKey() throws Exception {
        UserProfile profile = activeProfile("alice");
        when(userTokenCache.getUserProfileFromToken("computed-token")).thenReturn(profile);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("auth-token", "computed-token");
        request.addHeader("auth-username", "alice");
        request.addHeader("auth-key", "api-key");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertInstanceOf(LegacyUserTokenAuthentication.class,
                SecurityContextHolder.getContext().getAuthentication());
        verify(repository, never()).findByUser_UsernameIgnoreCase("alice");
        clearSecurityContext();
    }

    @Test
    void rejectsRequestWhenAuthenticationIsRequiredAndMissing() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(new MockHttpServletRequest(), response, chain);

        assertEquals(403, response.getStatus());
        assertNull(chain.getRequest());
    }

    private MockHttpServletRequest requestWithHeader(String name, String value) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(name, value);
        return request;
    }

    private UserProfile activeProfile(String username) {
        UserProfile profile = mock(UserProfile.class);
        profile.user = Principal.createStandardized(username, username + "@example.org");
        profile.active = true;
        when(profile.getRoles()).thenReturn(Collections.emptyList());
        when(profile.standardize()).thenReturn(profile);
        return profile;
    }

    private void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }
}
