package gsrs.security;

import ix.core.models.UserProfile;

public interface UserTokenCache {


//    String SESSION = "ix.session";

    void updateUserCache(UserProfile up);

    UserProfile getUserProfileFromToken(String token);

    void evictUser(UserProfile up);
}
