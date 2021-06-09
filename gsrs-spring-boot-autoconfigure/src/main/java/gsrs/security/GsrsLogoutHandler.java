package gsrs.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//@Service
public class GsrsLogoutHandler implements LogoutHandler {

    private final UserTokenCache userCache;

    public GsrsLogoutHandler(UserTokenCache userCache) {
        this.userCache = userCache;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response,
                       Authentication authentication) {
//        String userName = UserUtils.getAuthenticatedUserName();
//        userCache..evictUser(userName);
    }
}
