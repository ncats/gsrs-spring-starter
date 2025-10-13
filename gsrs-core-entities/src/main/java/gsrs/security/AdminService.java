package gsrs.security;


import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import gsrs.services.PrivilegeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import gov.nih.ncats.common.util.Unchecked;
import gsrs.repository.UserProfileRepository;
import ix.core.models.Role;

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
    //@hasAdminRole
    @canConfigureSystem
    public Authentication getCurrentAdminAuth(){
        return SecurityContextHolder.getContext().getAuthentication();
    }
    

    public Authentication getAnyAdmin(){
        return new UsernamePasswordAuthenticationToken(principalRepository.findAnAdminUsername().orElse("admin"), null,
                Arrays.stream(PrivilegeService.instance().getAllRoleNames().toArray(new String[0])).map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toList()));

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

    /**
     * Run the given Runnable with the given Authentication.
     * @param authentication the Authentication to use, if set to {@code null},
     *                        then it will run as someone not authenticated.
     * @param runnable the runnable to execute.
     * @throws NullPointerException if runnable is null.
     */
    public void runAs(Authentication authentication, Runnable runnable){
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
        _runAsAdmin(runnable, oldAuth, true);
    }

    private <E extends Throwable> void _runAsAdmin(Unchecked.ThrowingRunnable<E> runnable, Authentication oldAuth, boolean check) throws E {
        if(check && GsrsSecurityUtils.isAdmin(oldAuth)){
            //already an admin just run it
            runnable.run();
            return;
        }
        try {
            Authentication auth = new UsernamePasswordAuthenticationToken(principalRepository.findAnAdminUsername().orElse("admin"), null,
                    Arrays.stream(PrivilegeService.instance().getAllRoleNames().toArray(new String[0])).map(r -> new SimpleGrantedAuthority("ROLE_" + r)).collect(Collectors.toList()));

            SecurityContextHolder.getContext().setAuthentication(auth);
            runnable.run();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(oldAuth);
        }
    }

    private <E extends Throwable> void runAsAdmin(Unchecked.ThrowingRunnable<E> runnable, Authentication currentAuth) throws E {
        if(GsrsSecurityUtils.isAdmin(currentAuth)){
            //already an admin just run it
            runAs(currentAuth,runnable);
            return;
        }
        runAsAdmin(runnable);
    }


}
