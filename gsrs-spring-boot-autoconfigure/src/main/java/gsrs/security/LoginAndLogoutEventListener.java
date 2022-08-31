package gsrs.security;

import gsrs.cache.GsrsCache;
import gsrs.repository.SessionRepository;
import gsrs.repository.UserProfileRepository;
import gsrs.security.SessionConfiguration;
import ix.core.models.Session;
import ix.core.models.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import gov.nih.ncats.common.util.TimeUtil;
import java.util.List;
import java.util.Optional;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class LoginAndLogoutEventListener {

    @Autowired
    private SessionConfiguration sessionConfiguration;

    @EventListener
    @Transactional
    public void onLogin(AuthenticationSuccessEvent event) {
        // This method is called:
        // after user credentials are checked successfully

        UserProfile up = (UserProfile) event.getAuthentication().getPrincipal();

        Optional<Session> session = sessionConfiguration.cleanUpSessionsThenGetSession(up);

    }
}
