package gsrs.security;


import gov.nih.ncats.common.functions.ThrowableConsumer;
import gov.nih.ncats.common.util.Unchecked;
import gsrs.repository.UserProfileRepository;
import ix.core.models.Role;
import ix.core.models.UserProfile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

@Service
public class AdminService {


    public <E extends Throwable> void runAsAdmin(Unchecked.ThrowingRunnable<E> runnable) throws E {
        Authentication oldAuth = SecurityContextHolder.getContext().getAuthentication();
        try {
            Authentication auth = new UsernamePasswordAuthenticationToken("admin", null,
                    Arrays.stream(Role.values()).map(r -> new SimpleGrantedAuthority("ROLE_" + r.name())).collect(Collectors.toList()));

            SecurityContextHolder.getContext().setAuthentication(auth);
            runnable.run();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(oldAuth);
        }
    }


}
