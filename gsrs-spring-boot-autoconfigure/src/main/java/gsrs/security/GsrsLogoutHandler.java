package gsrs.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

//@Service
public class GsrsLogoutHandler implements LogoutHandler {

    private final UserTokenCache userCache;

    public GsrsLogoutHandler(UserTokenCache userCache) {
        this.userCache = userCache;
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response,
                       Authentication authentication) {
        HttpSession session = request.getSession();
        String sessionId = (String) session.getAttribute(UserTokenCache.SESSION);
//        String userName = UserUtils.getAuthenticatedUserName();
//        userCache..evictUser(userName);
    }
}
