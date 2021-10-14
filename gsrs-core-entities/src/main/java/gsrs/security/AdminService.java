package gsrs.security;


import gov.nih.ncats.common.functions.ThrowableConsumer;
import gov.nih.ncats.common.util.Unchecked;
import gsrs.repository.PrincipalRepository;
import gsrs.repository.UserProfileRepository;
import gsrs.springUtils.GsrsSpringUtils;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AdminService {

    @Autowired
    private UserProfileRepository principalRepository;

    /**
     * Get the {@code Authentication} object for the
     * current logged in Admin.
     * @apiNote This method will only work if the caller
     * IS an admin defined by @hasAdminRole
     * @return an Authentication; should never be null.
     */
    @hasAdminRole
    public Authentication getAdminAuth(){
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * Run the given Runnable with the given Authentication.
     * @param authentication the Authentication to use, if set to {@code null},
     *                        then it will run as someone not authenticated.
     * @param runnable the runnable to execute.
     * @param <E> the throwable that can be thrown.
     * @throws E
     * @throws NullPointerException if runnable is null.
     */
    public <E extends Throwable> void runAs(Authentication authentication, Unchecked.ThrowingRunnable<E> runnable) throws E {
        Objects.requireNonNull(runnable);
        Authentication oldAuth = SecurityContextHolder.getContext().getAuthentication();
        try {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            runnable.run();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(oldAuth);
        }
    }

    public <E extends Throwable> void runAsCurrentUser(Unchecked.ThrowingRunnable<E> runnable) throws E {
       runAs(SecurityContextHolder.getContext().getAuthentication(), runnable);
    }

    public <E extends Throwable> void runAsAdmin(Unchecked.ThrowingRunnable<E> runnable) throws E {
        Authentication oldAuth = SecurityContextHolder.getContext().getAuthentication();
        if(GsrsSecurityUtils.isAdmin(oldAuth)){
            //already an admin just run it
            runnable.run();
            return;
        }
        try {
            Authentication auth = new UsernamePasswordAuthenticationToken(principalRepository.findAnAdminUsername().orElse("admin"), null,
                    Arrays.stream(Role.values()).map(r -> new SimpleGrantedAuthority("ROLE_" + r.name())).collect(Collectors.toList()));

            SecurityContextHolder.getContext().setAuthentication(auth);
            runnable.run();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(oldAuth);
        }
    }


}
