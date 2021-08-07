package gsrs.startertests.service;

import gsrs.repository.GroupRepository;
import gsrs.repository.PrincipalRepository;
import gsrs.repository.UserProfileRepository;
import gsrs.services.*;
import gsrs.startertests.GsrsJpaTest;
import gsrs.startertests.jupiter.AbstractGsrsJpaEntityJunit5Test;
import ix.core.models.Group;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@GsrsJpaTest
@ActiveProfiles("test")
public class UserProfileServiceTest extends AbstractGsrsJpaEntityJunit5Test {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PrincipalRepository principalRepository;
    @Autowired
    private UserProfileRepository userProfileRepository;
    @Autowired
    private GroupRepository groupRepository;


    @Autowired
    private PlatformTransactionManager platformTransactionManager;
    @Autowired
    private UserProfileService userProfileService;

    @TestConfiguration
    @EnableGlobalMethodSecurity(prePostEnabled = true)
    static class MyTestConfig{
//
//        @Bean
//        public UserProfileService userProfileService(UserProfileRepository userProfileRepository, GroupService groupService){
//            return new UserProfileService(userProfileRepository, groupService);
//        }
//
//        @Bean
//        public PrincipalService principalService(PrincipalRepository principalRepository, TestEntityManager entityManager){
//            return new PrincipalServiceImpl(principalRepository, entityManager.getEntityManager());
//        }
//
//        @Bean
//        public GroupService groupService(GroupRepository groupRepository, TestEntityManager entityManager){
//            return new GroupServiceImpl(groupRepository, entityManager.getEntityManager());
//        }
//
    }

    @Test

    public void notloggedInCreateUserShouldErrorOut(){
        Assertions.assertThrows(AuthenticationException.class, ()->{
            UserProfileService.NewUserRequest request =  UserProfileService.NewUserRequest.builder()
                    .username("myUser")
                    .build();
            System.out.println("request = " + request);
            userProfileService.createNewUserProfile(request.createValidatedNewUserRequest());
        });

    }

    @Test
    @WithMockUser(username = "user", roles="Query")
    public void notAdminInCreateUserShouldErrorOut(){
        Assertions.assertThrows(AccessDeniedException.class, ()->{
            UserProfileService.NewUserRequest request =  UserProfileService.NewUserRequest.builder()
                    .username("myUser")
                    .build();
            System.out.println("request = " + request);
            userProfileService.createNewUserProfile(request.createValidatedNewUserRequest());
        });

    }

    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void createUser(){
        UserProfileService.NewUserRequest request =  UserProfileService.NewUserRequest.builder()
                .username("myUser")
                .build();
        UserProfile up = userProfileService.createNewUserProfile(request.createValidatedNewUserRequest());
        assertEquals("myUser", up.user.username);
        assertNotNull(up.id);
        assertNotNull(up.user.id);

        assertFalse(up.active);
        assertEquals(new ArrayList<>(), up.getRoles());

        assertEquals(1, userProfileRepository.count());
        assertEquals(1, principalRepository.count());
        assertEquals(0, groupRepository.count());
    }
    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void createActiveUser(){
        UserProfileService.NewUserRequest request =  UserProfileService.NewUserRequest.builder()
                .username("myUser")
                .isActive(true)
                .build();
        UserProfile up = userProfileService.createNewUserProfile(request.createValidatedNewUserRequest());
        assertEquals("myUser", up.user.username);
        assertNotNull(up.id);
        assertNotNull(up.user.id);

        assertTrue(up.active);
        assertEquals(new ArrayList<>(), up.getRoles());

        assertEquals(1, userProfileRepository.count());
        assertEquals(1, principalRepository.count());
        assertEquals(0, groupRepository.count());
    }

    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void createUserWithRoles(){
        List<Role> roles = Role.roles(Role.Query, Role.Updater);
        UserProfileService.NewUserRequest request =  UserProfileService.NewUserRequest.builder()
                .username("myUser")
                .roles(roles.stream().map(Role::name).collect(Collectors.toSet()))
                .build();
        UserProfile up = userProfileService.createNewUserProfile(request.createValidatedNewUserRequest());
        assertEquals("myUser", up.user.username);
        assertNotNull(up.id);
        assertNotNull(up.user.id);
        assertEquals(roles, up.getRoles());
        assertEquals(1, userProfileRepository.count());
        assertEquals(1, principalRepository.count());

        assertFalse(up.acceptPassword("fakepass"));
    }

    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void createUserWithGroups(){

        UserProfileService.NewUserRequest request =  UserProfileService.NewUserRequest.builder()
                .username("myUser")
                .groups(Arrays.asList("group1", "group2").stream().collect(Collectors.toSet()))
                .build();
        UserProfile up = userProfileService.createNewUserProfile(request.createValidatedNewUserRequest());
        assertEquals("myUser", up.user.username);
        assertNotNull(up.id);
        assertNotNull(up.user.id);

        Group g1 = groupRepository.findByName("group1");
        assertTrue(g1.getMembers().contains(up.user));
        Group g2 = groupRepository.findByName("group2");
        assertTrue(g2.getMembers().contains(up.user));

        assertEquals(1, userProfileRepository.count());
        assertEquals(1, principalRepository.count());
        assertEquals(2, groupRepository.count());

        List<Group> groups = groupRepository.findGroupsByMembers(up.user);
        assertEquals(2, groups.size());
        assertThat(groups).contains(g1, g2);

    }
    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void createUserSetPassword(){
        UserProfileService.NewUserRequest request =  UserProfileService.NewUserRequest.builder()
                .username("myUser")
                .password("mypass")
                .build();
        UserProfile up = userProfileService.createNewUserProfile(request.createValidatedNewUserRequest());
        assertEquals("myUser", up.user.username);
        assertNotNull(up.id);
        assertNotNull(up.user.id);

        assertTrue(up.acceptPassword("mypass"));

    }

    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void modifyUserChangePassword(){
        UserProfileService.NewUserRequest request =  UserProfileService.NewUserRequest.builder()
                .username("myUser")
                .password("mypass")
                .build();
        UserProfile up = userProfileService.createNewUserProfile(request.createValidatedNewUserRequest());
        assertEquals("myUser", up.user.username);
        assertNotNull(up.id);
        assertNotNull(up.user.id);

        assertTrue(up.acceptPassword("mypass"));
        assertFalse(up.acceptPassword("newPass"));

        UserProfile up2 = userProfileService.updateUserProfile(UserProfileService.NewUserRequest.builder()
                .username("myUser")
                .password("newPass")
                .build().createValidatedNewUserRequest());

        assertEquals(up2.id,up.id);
        assertEquals(up2.user.id, up.user.id);

        assertFalse(up2.acceptPassword("mypass"));
        assertTrue(up2.acceptPassword("newPass"));

    }

    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void modifyUserAddGroupFromNone(){
        UserProfileService.NewUserRequest request =  UserProfileService.NewUserRequest.builder()
                .username("myUser")
                .password("mypass")
                .build();
        UserProfile up = userProfileService.createNewUserProfile(request.createValidatedNewUserRequest());
        assertEquals("myUser", up.user.username);
        assertNotNull(up.id);
        assertNotNull(up.user.id);

        assertTrue(up.acceptPassword("mypass"));
        assertFalse(up.acceptPassword("newPass"));

        UserProfile up2 = userProfileService.updateUserProfile(UserProfileService.NewUserRequest.builder()
                .username("myUser")
                .groups(Arrays.asList("group1").stream().collect(Collectors.toSet()))
                .build().createValidatedNewUserRequest());

        assertEquals(up2.id,up.id);
        assertEquals(up2.user.id, up.user.id);

        Group g = groupRepository.findByName("group1");
        assertTrue(g.getMembers().contains(up2.user));

        List<Group> groups = groupRepository.findGroupsByMembers(up2.user);
        assertEquals(1, groups.size());
        assertEquals(Arrays.asList(g), groups);
    }

    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void modifyUserAddGroupFromAlreadyExistingList(){
        UserProfileService.NewUserRequest request =  UserProfileService.NewUserRequest.builder()
                .username("myUser")
                .password("mypass")
                .groups(Arrays.asList("group1", "group2").stream().collect(Collectors.toSet()))
                .build();
        UserProfile up = userProfileService.createNewUserProfile(request.createValidatedNewUserRequest());
        assertEquals("myUser", up.user.username);
        assertNotNull(up.id);
        assertNotNull(up.user.id);

        assertTrue(up.acceptPassword("mypass"));
        assertFalse(up.acceptPassword("newPass"));

        UserProfile up2 = userProfileService.updateUserProfile(UserProfileService.NewUserRequest.builder()
                .username("myUser")
                .groups(Arrays.asList("group1", "group2", "group3").stream().collect(Collectors.toSet()))
                .build().createValidatedNewUserRequest());

        assertEquals(up2.id,up.id);
        assertEquals(up2.user.id, up.user.id);

        Group g1 = groupRepository.findByName("group1");
        assertTrue(g1.getMembers().contains(up2.user));

        Group g2 = groupRepository.findByName("group2");
        assertTrue(g2.getMembers().contains(up2.user));
        Group g3 = groupRepository.findByName("group3");
        assertTrue(g3.getMembers().contains(up2.user));

        List<Group> groups = groupRepository.findGroupsByMembers(up2.user);
        assertEquals(3, groups.size());
        assertThat(groups).contains(g1, g2, g3);

    }

    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void modifyUserRemoveGroupFromAlreadyExistingList(){
        UserProfileService.NewUserRequest request =  UserProfileService.NewUserRequest.builder()
                .username("myUser")
                .password("mypass")
                .groups(Arrays.asList("group1", "group2").stream().collect(Collectors.toSet()))
                .build();
        UserProfile up = userProfileService.createNewUserProfile(request.createValidatedNewUserRequest());
        assertEquals("myUser", up.user.username);
        assertNotNull(up.id);
        assertNotNull(up.user.id);

        assertTrue(up.acceptPassword("mypass"));
        assertFalse(up.acceptPassword("newPass"));

        UserProfile up2 = userProfileService.updateUserProfile(UserProfileService.NewUserRequest.builder()
                .username("myUser")
                .groups(Arrays.asList( "group2").stream().collect(Collectors.toSet()))
                .build().createValidatedNewUserRequest());

        assertEquals(up2.id,up.id);
        assertEquals(up2.user.id, up.user.id);


        Group g2 = groupRepository.findByName("group2");
        assertTrue(g2.getMembers().contains(up2.user));

        Group g1 = groupRepository.findByName("group1");
        assertFalse(g1.getMembers().contains(up2.user));


        List<Group> groups = groupRepository.findGroupsByMembers(up2.user);
        assertEquals(1, groups.size());
        assertThat(groups).contains(g2);

    }

    @Test
    @WithMockUser(username = "admin", roles="Admin")
    public void modifyUserRemoveAndAddGroupsFromAlreadyExistingList(){
        UserProfileService.NewUserRequest request =  UserProfileService.NewUserRequest.builder()
                .username("myUser")
                .password("mypass")
                .groups(Arrays.asList("group1", "group2").stream().collect(Collectors.toSet()))
                .build();
        UserProfile up = userProfileService.createNewUserProfile(request.createValidatedNewUserRequest());
        assertEquals("myUser", up.user.username);
        assertNotNull(up.id);
        assertNotNull(up.user.id);

        assertTrue(up.acceptPassword("mypass"));
        assertFalse(up.acceptPassword("newPass"));

        UserProfile up2 = userProfileService.updateUserProfile(UserProfileService.NewUserRequest.builder()
                .username("myUser")
                .groups(Arrays.asList("group2", "group3").stream().collect(Collectors.toSet()))
                .build().createValidatedNewUserRequest());

        assertEquals(up2.id,up.id);
        assertEquals(up2.user.id, up.user.id);

        Group g1 = groupRepository.findByName("group1");
        assertFalse(g1.getMembers().contains(up2.user));

        Group g2 = groupRepository.findByName("group2");
        assertTrue(g2.getMembers().contains(up2.user));
        Group g3 = groupRepository.findByName("group3");
        assertTrue(g3.getMembers().contains(up2.user));

        List<Group> groups = groupRepository.findGroupsByMembers(up2.user);
        assertEquals(2, groups.size());
        assertThat(groups).contains( g2, g3);

    }
}
