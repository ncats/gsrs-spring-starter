package gsrs.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@EnableWebSecurity(debug = true)
@EnableGlobalMethodSecurity(
        securedEnabled = true,
        prePostEnabled = true,
        jsr250Enabled = true
)
public class LegacyGsrsSecurityConfiguration extends WebSecurityConfigurerAdapter {
    @Autowired
    private LegacyGsrsAuthenticationProvider legacyGsrsAuthenticationProvider;

   @Autowired
   private LegacyAuthenticationFilter legacyAuthenticationFilter;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
//        http.authorizeRequests()
//                .antMatchers("/**").hasRole("USER")
//                .anyRequest()
//                .authenticated()
//                .and()
//                .formLogin()
        http
//                .authorizeRequests()
//                .and()
//                .antMatchers("/api/**")
//                .permitAll()
//                .and()
                .addFilterBefore(legacyAuthenticationFilter, BasicAuthenticationFilter.class)

        ;

    }

    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.authenticationProvider(legacyGsrsAuthenticationProvider);
    }
}
