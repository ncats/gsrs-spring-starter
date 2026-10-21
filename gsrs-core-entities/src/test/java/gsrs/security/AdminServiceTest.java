package gsrs.security;

import gsrs.repository.UserProfileRepository;
import gsrs.services.PrivilegeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mockStatic;

class AdminServiceTest {

    private UserProfileRepository repository;
    private AdminService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserProfileRepository.class);
        service = new AdminService();
        ReflectionTestUtils.setField(service, "principalRepository", repository);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void runAsInstallsAuthenticationForTheRunnableAndRestoresTheOriginal() {
        Authentication original = authentication("original");
        Authentication replacement = authentication("replacement");
        SecurityContextHolder.getContext().setAuthentication(original);
        AtomicReference<Authentication> observed = new AtomicReference<>();

        service.runAs(replacement, (Runnable) () ->
                observed.set(SecurityContextHolder.getContext().getAuthentication()));

        assertSame(replacement, observed.get());
        assertSame(original, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void runAsRestoresTheOriginalAuthenticationWhenTheRunnableThrows() {
        Authentication original = authentication("original");
        Authentication replacement = authentication("replacement");
        SecurityContextHolder.getContext().setAuthentication(original);

        assertThrows(IllegalStateException.class,
                () -> service.runAs(replacement, (Runnable) () -> {
                    assertSame(replacement, SecurityContextHolder.getContext().getAuthentication());
                    throw new IllegalStateException("failure");
                }));

        assertSame(original, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void runAsCurrentUserUsesTheCurrentAuthenticationAndRestoresIt() {
        Authentication original = authentication("original");
        SecurityContextHolder.getContext().setAuthentication(original);
        AtomicReference<Authentication> observed = new AtomicReference<>();

        service.runAsCurrentUser(() -> observed.set(SecurityContextHolder.getContext().getAuthentication()));

        assertSame(original, observed.get());
        assertSame(original, SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void getCurrentAdminAuthReturnsTheContextAuthentication() {
        Authentication authentication = authentication("admin");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertSame(authentication, service.getCurrentAdminAuth());
    }

    @Test
    void getAnyAdminBuildsAnAuthenticationWithConfiguredRoleAuthorities() {
        PrivilegeService privilegeService = mock(PrivilegeService.class);
        when(repository.findAnAdminUsername()).thenReturn(Optional.of("configured-admin"));
        when(privilegeService.getAllRoleNames()).thenReturn(List.of("Admin", "Query"));

        try (var mockedPrivilegeService = mockStatic(PrivilegeService.class)) {
            mockedPrivilegeService.when(PrivilegeService::instance).thenReturn(privilegeService);

            Authentication authentication = service.getAnyAdmin();

            assertEquals("configured-admin", authentication.getName());
            assertTrue(authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_Admin")));
            assertTrue(authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_Query")));
        }
    }

    private Authentication authentication(String name) {
        return new UsernamePasswordAuthenticationToken(name, "credentials");
    }
}
