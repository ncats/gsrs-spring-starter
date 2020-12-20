package gsrs.security;

import gsrs.repository.UserProfileRepository;
import ix.core.models.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class GsrsUserProfileUserService implements UserDetailsService {
    @Autowired
    private UserProfileRepository userProfileRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserProfile up = userProfileRepository.findByUser_Username(username);
        if(up ==null){
            throw new UsernameNotFoundException(username);
        }
        return new UserProfileUserDetails(up);
    }
}
