package gsrs.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import gsrs.controller.GsrsControllerConfiguration;
import gsrs.controller.GsrsRestResponseErrorHandler;

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
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.request.async.WebAsyncManagerIntegrationFilter;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true,
        proxyTargetClass = true,
        prePostEnabled = true)
@Configuration
public class LegacyGsrsSecurityConfiguration extends WebSecurityConfigurerAdapter {

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
    

    //TODO this is the default session cookie name Spring uses or should we just use ix.session
    @Value("${gsrs.sessionKey}")
    private String sessionCookieName;

    private ObjectMapper mapper = new ObjectMapper();
    
    //    @Autowired
//    LegacyAuthenticationFilter legacyAuthenticationFilter;
    @Bean
    public HttpFirewall allowUrlEncodedPercentHttpFirewall() {
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
    @Override
    public void configure(WebSecurity web) throws Exception {
        super.configure(web);
        web.httpFirewall(allowUrlEncodedPercentHttpFirewall());

    }
    @Bean
    public LegacyAuthenticationFilter legacyAuthenticationFilter(){
        return new LegacyAuthenticationFilter();
    }
    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http
                .addFilterBefore(legacyAuthenticationFilter(), LogoutFilter.class)
                .authorizeRequests()
                .antMatchers(HttpMethod.DELETE, "/api/*").authenticated()
                .antMatchers(HttpMethod.PUT, "/api/*").authenticated()
                .antMatchers(HttpMethod.POST, "/api/*").authenticated()
//                .antMatchers(HttpMethod.GET, "/logout").authenticated()
                
//        .antMatchers("/login*").permitAll()

//                .anyRequest().authenticated()
                .and()
               
//                .and()
//                .authenticationProvider(legacyGsrsAuthenticationProvider)
//                .and().httpBasic()
//                .and().formLogin();
//                .and()
                .csrf().disable()

                .exceptionHandling().accessDeniedHandler(accessDeniedHandler())
                .and().exceptionHandling().authenticationEntryPoint(unauthorizedEntryPoint())
        .and()
        
//        .sessionManagement().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.NEVER)
                .and()
            .logout()
                .logoutUrl("/logout")
                .deleteCookies(sessionCookieName)
                .addLogoutHandler(logoutHandler)
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK))
                
                .permitAll()
                .and()
            .formLogin()
                .successHandler(legacyGsrsAuthenticationSuccessHandler)
                .loginProcessingUrl("/api/v1/whoami")
                .failureHandler(failureHandler())
                .and()
//                .formLogin()
//                .disable()
        ;
                ;

        ;
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
    //    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return NoOpPasswordEncoder.getInstance();
//    }
//    @Bean
//    public UserDetailsService userDetailsService() {
//
////        User.UserBuilder users = User.builder();
//        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
//        manager.createUser(User.builder().username("user").password("{noop}password").roles(Role.Query.name()).build());
//        manager.createUser(User.builder().username("admin").password("{noop}admin").roles( Role.Admin.name()).build());
//        return manager;
//
//    }
//    @Override
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//        auth.authenticationProvider(legacyGsrsAuthenticationProvider);
//    }


//    @Bean
//    public GsrsUserProfileUserService gsrsUserProfileUserService(){
//        return new GsrsUserProfileUserService();
//    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationEventPublisher(defaultAuthenticationEventPublisher);
        auth.eraseCredentials(false);
        auth.authenticationProvider(legacyGsrsAuthenticationProvider);
//        auth.userDetailsService(gsrsUserProfileUserService());
//        auth.inMemoryAuthentication().withUser("admin").password("{noop}admin").roles(Role.Admin.name());
//        auth.inMemoryAuthentication().withUser("user1").password("{noop}pass").roles(Role.Query.name());
//        auth.inMemoryAuthentication().withUser("user2").password("{noop}pass").roles(Role.SuperUpdate.name());
//        User.UserBuilder users = User.withDefaultPasswordEncoder();
//
//        auth.inMemoryAuthentication().wi("admin").password("admin").roles(Role.Admin.name());
//        auth.inMemoryAuthentication().withUser("user1").password("pass").roles(Role.Query.name());
//        auth.inMemoryAuthentication().withUser("user2").password("pass").roles(Role.SuperUpdate.name());

    }
}
