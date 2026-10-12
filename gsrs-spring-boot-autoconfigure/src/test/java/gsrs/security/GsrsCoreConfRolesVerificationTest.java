package gsrs.security;

import com.github.zeldigas.spring.env.HoconPropertySourceLoader;
import gsrs.services.PrivilegeService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Empirical verification that the role/privilege configuration placed in
 * {@code gsrs-core.conf} is really parsed by the HOCON loader and that every
 * configured role resolves its configured privileges when driven through
 * {@link PrivilegeService}.
 */
public class GsrsCoreConfRolesVerificationTest {

    private UserRoleConfiguration loadFromGsrsCoreConf() throws IOException {
        PropertySourceLoader loader = new HoconPropertySourceLoader();
        ClassPathResource resource = new ClassPathResource("gsrs-core.conf");
        assertTrue(resource.exists(), "gsrs-core.conf must be on the classpath");

        List<PropertySource<?>> sources = loader.load("gsrs-core", resource);
        assertFalse(sources.isEmpty(), "HOCON loader returned no property sources for gsrs-core.conf");

        StandardEnvironment environment = new StandardEnvironment();
        sources.forEach(environment.getPropertySources()::addFirst);

        // proves the HOCON file was actually parsed (not silently ignored)
        assertNotNull(environment.getProperty("gsrs.security.info.roles.Query.privileges[0]"),
                "gsrs.security.info.roles was not present after loading gsrs-core.conf via HOCON");

        UserRoleConfiguration config = Binder.get(environment)
                .bind("gsrs.security.info", Bindable.of(UserRoleConfiguration.class))
                .get();
        assertNotNull(config, "Could not bind UserRoleConfiguration from gsrs-core.conf");
        return config;
    }

    private PrivilegeService newService(UserRoleConfiguration config) {
        PrivilegeService service = new PrivilegeService();
        ReflectionTestUtils.setField(service, "configuration", config);
        return service;
    }

    @Test
    void gsrsCoreConfActuallyDefinesRoles() throws IOException {
        UserRoleConfiguration config = loadFromGsrsCoreConf();
        assertNotNull(config.getRoles(), "roles should not be null");
        System.out.println("Loaded " + config.getRoles().size() + " roles from gsrs-core.conf:");
        config.getRoles().forEach((name, r) ->
                System.out.println("  role=" + name + " privileges=" + r.getPrivileges() + " include=" + r.getInclude()));
        assertEquals(4, config.getRoles().size());
        assertTrue(config.getRoles().containsKey("Query"), "Query role should be keyed by name");
        assertTrue(config.getRoles().containsKey("DataEntry"), "DataEntry role should be keyed by name");
        assertTrue(config.getRoles().containsKey("Approver"), "Approver role should be keyed by name");
        assertTrue(config.getRoles().containsKey("Admin"), "Admin role should be keyed by name");
    }

    @Test
    void everyConfiguredRoleResolvesItsConfiguredPrivileges() throws IOException {
        UserRoleConfiguration config = loadFromGsrsCoreConf();
        PrivilegeService service = newService(config);

        assertEquals(config.getRoles().size(), service.getAllRoleNames().size());
        config.getRoles().forEach((roleName, role) -> {
            List<String> effective = service.getPrivilegesForConfiguredRole(roleName);
            // every directly-configured privilege must be present in the effective set
            for (String privilege : role.getPrivileges()) {
                assertTrue(effective.contains(privilege),
                        "Role " + roleName + " should have privilege '" + privilege + "' but got " + effective);
                assertTrue(service.canRolePerform(roleName, privilege),
                        "canRolePerform(" + roleName + ", " + privilege + ") should be true");
            }
            System.out.println("  role=" + roleName + " effectivePrivileges(" + effective.size() + ")=" + effective);
        });
    }

    @Test
    void canonicalRolesAndInheritanceAreWired() throws IOException {
        UserRoleConfiguration config = loadFromGsrsCoreConf();
        PrivilegeService service = newService(config);

        assertEquals(4, service.getAllRoleNames().size());
        assertTrue(service.canRolePerform("Admin", "Manage Users"));
        assertTrue(service.canRolePerform("Approver", "Approve Records"));
        assertTrue(service.canRolePerform("DataEntry", "Edit"));
        assertTrue(service.canRolePerform("Query", "Search"));

        // the map key must be propagated onto the value's name
        assertEquals("Admin", service.getConfiguredRolesByName().get("Admin").getName(),
                "role name should equal the map key");
        assertEquals("DataEntry", service.getConfiguredRolesByName().get("DataEntry").getName(),
                "role name should equal the map key");

        // inheritance via `include`
        assertTrue(service.canRolePerform("DataEntry", "Search"), "DataEntry should inherit Query privileges");
        assertTrue(service.canRolePerform("Approver", "Edit"), "Approver should inherit DataEntry privileges");
        assertTrue(service.canRolePerform("Admin", "Approve Records"), "Admin should inherit Approver privileges");

        // negative control
        assertFalse(service.canRolePerform("Query", "Manage Users"));
        assertFalse(service.canRolePerform("DataEntry", "Manage Users"));
    }
}
