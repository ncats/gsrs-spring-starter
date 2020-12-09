package gsrs.security;

import gsrs.repository.PrincipalRepository;
import gsrs.repository.UserProfileRepository;
import ix.core.models.Principal;
import ix.core.models.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;

//@Component
public class LegacyAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private LegacyAuthenticationConfiguration authenticationConfiguration;
    @Autowired
    private UserProfileRepository repository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

//    }
//
//    @Override
//    public void doFilter(ServletRequest servletRequest, ServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        //the order of the old GSRS is to check this:
        /*
        AuthenticatorFactory authFac = AuthenticatorFactory.getInstance(app);
		authFac.registerAuthenticator(new TrustHeaderAuthenticator());
		authFac.registerAuthenticator(new UserPasswordAuthenticator());
		authFac.registerAuthenticator(new UserTokenAuthenticator());
		authFac.registerAuthenticator(new UserKeyAuthenticator());
         */
//        HttpServletRequest request = (HttpServletRequest) servletRequest;
        Authentication auth = null;
        if(authenticationConfiguration.isTrustheader()){

            String username = request.getHeader(authenticationConfiguration.getUsernameheader());
            String email = request.getHeader(authenticationConfiguration.getUseremailheader());
            if(username !=null && email !=null){
                UserProfile up = repository.findByUser_Username(username);
                if(up ==null && authenticationConfiguration.isAutoregister()){
                    Principal p = new Principal(username, email);
                    up = new UserProfile(p);
                    if(authenticationConfiguration.isAutoregisteractive()){
                        up.active = true;
                    }
                    up.systemAuth = false;
                    //should cascade new Principal
                    repository.saveAndFlush(up);
                }
                if(up !=null){

                   auth= new UsernamePasswordAuthenticationToken(username, email,
                            up.getRoles().stream().map(r->new SimpleGrantedAuthority("ROLE_"+ r.name())).collect(Collectors.toList()));
                }
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
                UserProfile up = repository.findByUser_Username(username);
                if(up ==null && authenticationConfiguration.isAutoregister()) {
                    Principal p = new Principal(username, null);
                    up = new UserProfile(p);
                    if (authenticationConfiguration.isAutoregisteractive()) {
                        up.active = true;
                    }
                    up.systemAuth = false;
                    //should cascade new Principal
                    repository.saveAndFlush(up);

                }
                if(up!=null){
                    if(up.acceptPassword(pass)){
                        //valid password!

                        auth= new UsernamePasswordAuthenticationToken(username, up.getEncodePassword(),
                                up.getRoles().stream().map(r->new SimpleGrantedAuthority("ROLE_"+ r.name())).collect(Collectors.toList()));

                    }else{
                        throw new BadCredentialsException("invalid credentials for username" + username);
                    }

                }
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
