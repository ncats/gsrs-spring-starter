package gsrs.security;

import ix.core.models.UserProfile;

import java.util.Collections;
import java.util.List;

public interface UserTokenCache {


//    String SESSION = "ix.session";

    void updateUserCache(UserProfile up);

    UserProfile getUserProfileFromToken(String token);

    void evictUser(UserProfile up);

    default List<String> getTokenFingerprints(UserProfile up) {
        return Collections.emptyList();
    }
}
