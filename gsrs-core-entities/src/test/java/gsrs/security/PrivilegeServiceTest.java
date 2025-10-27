package gsrs.security;

import gsrs.services.PrivilegeService;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.Mockito.*;

@ActiveProfiles("test")
public class PrivilegeServiceTest {

    @ParameterizedTest
    @MethodSource("inputData")
    void testCanUserDoSomething(String thingToDo, Role userRole, UserRoleConfiguration.PermissionResult expectedResult) {
        PrivilegeService service = new PrivilegeService();
        UserProfile user1 = mock(UserProfile.class);
        List<Role> userRoles = Collections.singletonList(userRole);
        try (MockedStatic<GsrsSecurityUtils> mockedSecurityContext = mockStatic(GsrsSecurityUtils.class)) {
            mockedSecurityContext.when(GsrsSecurityUtils::getCurrentUser).thenReturn(user1);
            when(user1.getRoles()).thenReturn(userRoles);
            UserRoleConfiguration.PermissionResult canUser = service.canUserPerform(thingToDo);
            Assertions.assertEquals(expectedResult, canUser);
        }
    }

    private static Stream<Arguments> inputData() {
        return Stream.of(
                Arguments.of("Login", Role.of("Query"),UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Login", Role.of("DataEntry"),UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Login", Role.of("Approver"),UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Login", Role.of("Admin"),UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Approve Records", Role.of("Query"),UserRoleConfiguration.PermissionResult.MayNotPerform),
                Arguments.of("Approve Records", Role.of("DataEntry"),UserRoleConfiguration.PermissionResult.MayNotPerform),
                Arguments.of("Approve Records", Role.of("Approver"),UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Approve Records", Role.of("Admin"),UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Edit", Role.of("Query"),UserRoleConfiguration.PermissionResult.MayNotPerform),
                Arguments.of("Edit", Role.of("DataEntry"),UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("edit", Role.of("Approver"),UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Edit", Role.of("Admin"),UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Search", Role.of("Query"),UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Search", Role.of("DataEntry"),UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Search", Role.of("Approver"),UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Search", Role.of("Admin"),UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Manage Vocabularies", Role.of("Approver"),UserRoleConfiguration.PermissionResult.MayNotPerform),
                Arguments.of("Manage Vocabularies", Role.of("Query"),UserRoleConfiguration.PermissionResult.MayNotPerform),
                Arguments.of("Manage Vocabularies", Role.of("DataEntry"),UserRoleConfiguration.PermissionResult.MayNotPerform),
                Arguments.of("Manage Vocabularies", Role.of("Admin"),UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Manage Users", Role.of("Approver"),UserRoleConfiguration.PermissionResult.MayNotPerform),
                Arguments.of("Manage Users", Role.of("Query"),UserRoleConfiguration.PermissionResult.MayNotPerform),
                Arguments.of("Manage Users", Role.of("DataEntry"),UserRoleConfiguration.PermissionResult.MayNotPerform),
                Arguments.of("Manage Users", Role.of("Admin"),UserRoleConfiguration.PermissionResult.MayPerform)
        );
    }

    @Test
    void getPrivilegesForConfiguredRoleTest() {
        String startingRole = "DataEntry";
        String[] expectedPrivileges = {"Create", "Edit", "Login", "Search", "Browse", "Export Data","Export Relationships", "Save Record JSON" };
        PrivilegeService service = new PrivilegeService();
        List<String> actualPrivs = service.getPrivilegesForConfiguredRole(startingRole);
        String[] actualPrivileges = actualPrivs.toArray(new String[actualPrivs.size()]);
        Arrays.sort(expectedPrivileges);
        Arrays.sort(actualPrivileges);
        Assert.assertArrayEquals(expectedPrivileges, actualPrivileges);
    }

    @Test
    void getPrivilegesForConfiguredRoleTest2() {
        String startingRole = "Admin";
        String[] expectedPrivileges = {"Create", "Edit", "Login", "Search", "Browse", "Export Data", "Approve Records", "Edit Public Data",
                "Delete Lower Level Items", "Export Relationships", "Restore Previous Versions", "Save Record JSON",
                "Manage Users", "Manage Vocabularies", "Configure System", "Manage CVs", "Import Data", "Index Data", "Run Backup",
                "Run Tasks", "Override Duplicate Checks", "Merge Subconcepts","Modify Relationships", "Make Records Public",
                "Edit Approved Records", "Edit Approval IDs", "Manage Others Lists", "View Files"};
        PrivilegeService service = new PrivilegeService();
        List<String> actualPrivs = service.getPrivilegesForConfiguredRole(startingRole);
        String[] actualPrivileges = actualPrivs.toArray(new String[actualPrivs.size()]);
        Arrays.sort(expectedPrivileges);
        Arrays.sort(actualPrivileges);
        Assert.assertArrayEquals(expectedPrivileges, actualPrivileges);
    }

    @Test
    void getAllRoles(){
        PrivilegeService service = new PrivilegeService();
        List<String> roles = service.getAllRoleNames();
        String[] expectedRoles = {"Query", "DataEntry", "Approver", "Admin"};
        String[] actualRoles = roles.toArray(new String[roles.size()]);
        Arrays.sort(expectedRoles);
        Arrays.sort(actualRoles);
        Assert.assertArrayEquals(expectedRoles, actualRoles);
    }
}
