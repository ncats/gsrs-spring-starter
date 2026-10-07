package gsrs.tasks;

import gov.nih.ncats.common.util.TimeUtil;
import gsrs.repository.GroupRepository;
import gsrs.repository.SessionRepository;
import gsrs.repository.UserProfileRepository;
import gsrs.scheduledTasks.ScheduledTaskInitializer;
import gsrs.scheduledTasks.SchedulerPlugin;
import gsrs.services.UserProfileService;
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

@Slf4j
public class UserProfileDisableWhenInactive extends ScheduledTaskInitializer {

    int inactiveDayLimit = 60;

    int getInactiveDaysToNotify = 50;

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

    private String gsrsUrl = "https://gsrs.ncats.nih.gov/ginas/app/ui/";

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
                if(days >= getInactiveDaysToNotify && days >= inactiveDayLimit){
                    notifyUserOfExpiration(up.user.email, days, (inactiveDayLimit-days-1), gsrsUrl);
                } else if( days > inactiveDayLimit) {
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
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.executeWithoutResult(status -> {
                UserProfile managed = userRepository.findById(profile.id).orElseThrow();
                managed.active = false;
                managed.setIsAllDirty();
                userRepository.saveAndFlush(managed);
            });
            log.trace("Profile {} made inactive", profile.user.username);
        } catch (Exception e) {
            log.error("Error making profile {} inactive",
                    profile.user.username, e);
        }

    }

    private boolean notifyUserOfExpiration(String email, long daysSinceLastAccess, long expiration, String url) {
        String message =
                String.format("Dear User, it has been %d days since you logged into GSRS. If you do not log in within the next %d days, your account will be disabled. Please use URL %s",
                daysSinceLastAccess, expiration, url);
        log.info("We will send this message {}", message);
        return true;
    }

}
