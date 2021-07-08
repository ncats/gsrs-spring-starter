package gsrs.security;

import gsrs.controller.GsrsRestResponseErrorHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
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
    //    @Autowired
//    LegacyAuthenticationFilter legacyAuthenticationFilter;
    @Bean
    public LegacyAuthenticationFilter legacyAuthenticationFilter(){
        return new LegacyAuthenticationFilter();
    }
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .authorizeRequests()
                .antMatchers(HttpMethod.DELETE, "/api/*").authenticated()
                .antMatchers(HttpMethod.PUT, "/api/*").authenticated()
                .antMatchers(HttpMethod.POST, "/api/*").authenticated()
//        .antMatchers("/login*").permitAll()

//                .anyRequest().authenticated()
                .and()
                .addFilterBefore(legacyAuthenticationFilter(), BasicAuthenticationFilter.class)
//                .and()
//                .authenticationProvider(legacyGsrsAuthenticationProvider)
//                .and().httpBasic()
//                .and().formLogin();
//                .and()
                .csrf().disable()

                .exceptionHandling().accessDeniedHandler(accessDeniedHandler())
                .and().exceptionHandling().authenticationEntryPoint(unauthorizedEntryPoint())
        .and()
//                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
//                .and()
            .logout()
                .logoutUrl("/logout")
                .addLogoutHandler(logoutHandler)
                .logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.OK))
                .permitAll()
                .and()
            .formLogin()
                .successHandler(legacyGsrsAuthenticationSuccessHandler)
                .loginProcessingUrl("/api/v1/whoami")
                .and()
//                .formLogin()
//                .disable()
        ;
                ;

        ;
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
