package gsrs.security;

import gsrs.repository.UserProfileRepository;
import ix.core.models.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
//@Component
public class LegacyAuthenticationFilter implements Filter {
    @Autowired
    private LegacyAuthenticationConfiguration authenticationConfiguration;



    @Override
    public void doFilter(ServletRequest r, ServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //the order of the old GSRS is to check this:
        /*
        AuthenticatorFactory authFac = AuthenticatorFactory.getInstance(app);
		authFac.registerAuthenticator(new TrustHeaderAuthenticator());
		authFac.registerAuthenticator(new UserPasswordAuthenticator());
		authFac.registerAuthenticator(new UserTokenAuthenticator());
		authFac.registerAuthenticator(new UserKeyAuthenticator());
         */
        HttpServletRequest request = (HttpServletRequest) r;
        Authentication auth = null;
        if(authenticationConfiguration.isTrustheader()){

            String username = request.getHeader(authenticationConfiguration.getUsernameheader());
            String email = request.getHeader(authenticationConfiguration.getUseremailheader());
            if(username !=null && email !=null){
                auth = new LegacySsoAuthentication(username, email);
            }
        }
        /*
        String user=r.getHeader("auth-username");
        String key=r.getHeader("auth-key");
         */
        if(auth ==null) {
            String username = request.getHeader("auth-username");
            String pass = request.getHeader("auth-password");
            if (username != null && pass != null) {
                auth = new LegacyUserPassAuthentication(username, pass);
            }
        }
        if(auth ==null) {
            String token = request.getHeader("auth-token");
            if(token !=null){
                auth = new LegacyUserTokenAuthentication(token);
            }
        }
        if(auth !=null) {
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        filterChain.doFilter(request, response);

    }
}
