package gsrs.tasks;

import gov.nih.ncats.common.util.TimeUtil;
import gsrs.repository.GroupRepository;
import gsrs.repository.SessionRepository;
import gsrs.repository.UserProfileRepository;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.services.UserProfileService;
import ix.core.models.Group;
import ix.core.models.Role;
import ix.core.models.Session;
import ix.core.models.UserProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class UserProfileDisableWhenInactive extends ScheduledTaskInitializer {

    int inactiveDayLimit = 60;

    private final static String USER_TO_KEEP = "ADMIN";

    @Autowired
    UserProfileRepository userRepository;

    @Autowired
    SessionRepository sessionRepository;

    @Autowired
    protected PlatformTransactionManager transactionManager;

    @Autowired
    private UserProfileService service;

    @Autowired
    private GroupRepository groupRepository;

    @Override
    public void run(SchedulerPlugin.JobStats stats, SchedulerPlugin.TaskListener l) {
        List<UserProfile> toMakeInactive = new ArrayList<>();
        userRepository.findAll().forEach(up ->{
            log.trace("retrieved UP {}", up.user.username);
            if(up.active && up.user.username != null && !up.user.username.equalsIgnoreCase(USER_TO_KEEP)) {
                //only examine active; no point in testing those that are already inactive
                long now = TimeUtil.getCurrentTimeMillis();
                List<Session> sessions = sessionRepository.getAllSessionsFor(up);
                if( sessions.isEmpty()) {
                    log.trace("no sessions found for this one");
                    return;
                }
                sessions.sort(
                        Comparator.comparingLong((Session s)->s.accessed).reversed()
                );
                long lastAccessed = sessions.get(0).accessed;
                long days = Duration.between(
                        Instant.ofEpochMilli(lastAccessed),
                        Instant.ofEpochMilli(now)
                ).toDays();
                log.trace("{} days since last access", days);
                if( days > inactiveDayLimit) {
                    log.info("to make inactive!");
                    toMakeInactive.add(up);
                } else {
                    log.info("to keep active!");
                }
            } else {
                log.trace("UP was not even active or was special");
            }
        });
        log.info("{} UPs to make inactive", toMakeInactive.size());
        toMakeInactive.forEach(this::makeUserProfileInactive);
    }

    @Override
    public String getDescription() {
        return String.format("Make all users that have not logged in within the last %d days inactive", inactiveDayLimit);
    }

    private void makeUserProfileInactive(UserProfile profile) {
        try {
            UserProfile managed = userRepository.findById(profile.id).orElseThrow();
            managed.active = false;
            managed.setIsAllDirty();
            List<Group> groups =groupRepository.findGroupsByMembers(profile.user);
            Set<String> groupNames = groups.stream()
                    .map(g->g.name)
                    .collect(Collectors.toSet());
            Set<String> roleNames = profile.getRoles().stream().map(Role::getRole).collect(Collectors.toSet());
            TransactionTemplate tx = new TransactionTemplate(transactionManager);

            UserProfileService.NewUserRequest request = new UserProfileService.NewUserRequest(profile.user.username, null,
                    profile.user.email, profile.user.isAdmin(), false, groupNames, roleNames);
            UserProfileService.ValidatedNewUserRequest validatedRequest= request.createValidatedNewUserRequest();
            tx.executeWithoutResult(a -> service.updateUserProfile(validatedRequest));
            log.trace("profile saved");
        } catch (Throwable t){
            log.error("Error saving UP: ", t);
        }
    }

}
