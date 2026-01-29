package gsrs.security;

import ix.core.models.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class GsrsSecurityUtils {
    /**
     * Get the current logged in user Object.
     * @return an Object which is {@code null} if the user is not logged in;
     */
    public static Object getCurrentUser(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth ==null){
            return null;
        }
        return auth.getPrincipal();
    }
    public static Optional<String> getCurrentUsername(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if(auth ==null){
            return Optional.empty();
        }
        return Optional.of(auth.getName());
    }

    @Deprecated
    public static boolean isAdmin(){
        return hasAnyRoles(Role.of("Admin"));
    }

    @Deprecated
    public static boolean isAdmin(Authentication auth){
        return hasAnyRoles(auth, "Admin");
    }
    public static boolean hasAnyRoles(Collection<Role> roles){
        return hasAnyRoles((String[]) roles.stream().map(r -> r.getRole()).toArray(size-> new String[size]));
    }
    public static boolean hasAnyRoles(Role...roles){
        return hasAnyRoles((String[]) Arrays.stream(roles).map(r -> r.getRole()).toArray(size-> new String[size]));
    }
    public static boolean hasAnyRoles(String...roles){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return hasAnyRoles(auth, roles);
    }

    private static boolean hasAnyRoles(Authentication auth, String... roles) {
        if(auth ==null){
            return false;
        }
        Set<String> set = Arrays.stream(roles).map(r-> "ROLE_"+r).collect(Collectors.toSet());
        for(GrantedAuthority ga : auth.getAuthorities()){
            if(set.contains(ga.getAuthority())){
                return true;
            }
        }
        return false;
    }
}
