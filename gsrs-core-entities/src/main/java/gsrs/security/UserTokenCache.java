package gsrs.security;

import ix.core.models.UserProfile;

public interface UserTokenCache {
    void updateUserCache(UserProfile up);

    UserProfile getUserProfileFromToken(String token);
}
