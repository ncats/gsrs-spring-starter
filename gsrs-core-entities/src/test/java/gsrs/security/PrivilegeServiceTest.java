package gsrs.security;

import gsrs.services.PrivilegeService;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.springframework.test.context.ActiveProfiles;

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
                Arguments.of("Login", Role.Query, UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Login", Role.DataEntry, UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Login", Role.Approver, UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Login", Role.Admin, UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Approve Records", Role.Query, UserRoleConfiguration.PermissionResult.MayNotPerform),
                Arguments.of("Approve Records", Role.DataEntry, UserRoleConfiguration.PermissionResult.MayNotPerform),
                Arguments.of("Approve Records", Role.Approver, UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Approve Records", Role.Admin, UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Edit", Role.Query, UserRoleConfiguration.PermissionResult.MayNotPerform),
                Arguments.of("Edit", Role.DataEntry, UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("edit", Role.Approver, UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Edit", Role.Admin, UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Search", Role.Query, UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Search", Role.DataEntry, UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Search", Role.Approver, UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Search", Role.Admin, UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Manage Vocabularies", Role.Approver, UserRoleConfiguration.PermissionResult.MayNotPerform),
                Arguments.of("Manage Vocabularies", Role.Query, UserRoleConfiguration.PermissionResult.MayNotPerform),
                Arguments.of("Manage Vocabularies", Role.DataEntry, UserRoleConfiguration.PermissionResult.MayNotPerform),
                Arguments.of("Manage Vocabularies", Role.Admin, UserRoleConfiguration.PermissionResult.MayPerform),
                Arguments.of("Manage Users", Role.Approver, UserRoleConfiguration.PermissionResult.MayNotPerform),
                Arguments.of("Manage Users", Role.Query, UserRoleConfiguration.PermissionResult.MayNotPerform),
                Arguments.of("Manage Users", Role.DataEntry, UserRoleConfiguration.PermissionResult.MayNotPerform),
                Arguments.of("Manage Users", Role.Admin, UserRoleConfiguration.PermissionResult.MayPerform)

        );
    }
}
