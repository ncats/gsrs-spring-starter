package gsrs.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.controller.GsrsRestResponseErrorHandler;

import ix.core.models.Session;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true,
        proxyTargetClass = true,
        prePostEnabled = true)
@Configuration
public class LegacyGsrsSecurityConfiguration {

    private RequestMatcher permited = new AntPathRequestMatcher("/api/v1/whoami", HttpMethod.GET.toString());

    @Autowired
    private LogoutHandler logoutHandler;
    @Autowired
    LegacyGsrsAuthenticationProvider legacyGsrsAuthenticationProvider;

    @Autowired
    DefaultAuthenticationEventPublisher defaultAuthenticationEventPublisher;

    @Autowired
    LegacyGsrsAuthenticationSuccessHandler legacyGsrsAuthenticationSuccessHandler;

    @Autowired
    GsrsControllerConfiguration gsrsControllerConfiguration;

    private LegacyAuthenticationConfiguration authenticationConfiguration;

    @Autowired
    private SessionConfiguration sessionConfiguration;

    private ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public void setAuthenticationConfiguration(LegacyAuthenticationConfiguration authenticationConfiguration) {
        this.authenticationConfiguration = authenticationConfiguration;
        if(authenticationConfiguration.isAllownonauthenticated()){
            List<RequestMatcher> secured = new ArrayList();
            secured.add(new AntPathRequestMatcher("/api/**", HttpMethod.DELETE.toString()));
            secured.add(new AntPathRequestMatcher("/api/**", HttpMethod.PUT.toString()));
            secured.add(new AntPathRequestMatcher("/api/**", HttpMethod.POST.toString()));
            secured.add(new AntPathRequestMatcher("/logout", HttpMethod.GET.toString()));
            this.permited = new NegatedRequestMatcher(new OrRequestMatcher(secured));
        }
    }

    @Bean
    public StrictHttpFirewall httpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        //to allow for url decoding
        firewall.setAllowUrlEncodedPercent(true);
        //our pojo pointer functions sometimes have semicolons
        firewall.setAllowSemicolon(true);
        //allow encoded slashes for file paths and some smiles strings
        firewall.setAllowUrlEncodedSlash(true);
//        firewall.setAllowUrlEncodedDoubleSlash(true);
        return firewall;
    }

    @Bean
    public LegacyAuthenticationFilter legacyAuthenticationFilter(){
        return new LegacyAuthenticationFilter();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        return http
            .addFilterBefore(legacyAuthenticationFilter(), LogoutFilter.class)
            .authorizeRequests((authorizeRequests) -> authorizeRequests
                .requestMatchers(permited).permitAll()
                .anyRequest().authenticated())
            .csrf((csrf) -> csrf.disable())
            .exceptionHandling((exceptionHandling) -> exceptionHandling.accessDeniedHandler(accessDeniedHandler()))
            .exceptionHandling((exceptionHandling) -> exceptionHandling.authenticationEntryPoint(unauthorizedEntryPoint()))
            .sessionManagement((sessionManagement) -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.NEVER))
            .logout((logout) -> logout
                .logoutUrl("/logout")
                .deleteCookies(sessionConfiguration.getSessionCookieName())
                .addLogoutHandler(logoutHandler)
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK)))
            .formLogin((formLogin) -> formLogin
                .successHandler(legacyGsrsAuthenticationSuccessHandler)
                .loginProcessingUrl("/api/v1/whoami")
                .failureHandler(failureHandler()))
            .build();
    }

    private AuthenticationFailureHandler failureHandler() {

        return (httpServletRequest, httpServletResponse, e) -> {
            //use the same GSRS response JSON format and allow of overriding status/error codes
            int statusCode = gsrsControllerConfiguration.getHttpStatusFor(HttpStatus.NOT_FOUND, httpServletRequest).value();
            Object response = GsrsControllerConfiguration.createStatusJson("No user logged in", statusCode);

            httpServletResponse.getWriter().append(mapper.writer().writeValueAsString(response));
            httpServletResponse.setStatus(statusCode);
            httpServletResponse.setContentType("application/json");
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler(){
        return new GsrsRestResponseErrorHandler();
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return new GsrsAuthenticationEntryPoint();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationEventPublisher(defaultAuthenticationEventPublisher);
        auth.eraseCredentials(false);
        auth.authenticationProvider(legacyGsrsAuthenticationProvider);
    }
}
