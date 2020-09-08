package edu.iu.uits.lms.courselist.config;

import edu.iu.uits.lms.lti.security.LtiAuthenticationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@Slf4j
public class SecurityConfig {

    @Configuration
    @Order(1)
    public static class CourseListLtiSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.authorizeRequests(authorizeRequests ->
                  authorizeRequests
                        .antMatchers("/lti", "/remote/**").hasAnyRole("ANONYMOUS", LtiAuthenticationProvider.LTI_USER)
                        .anyRequest().authenticated());

            //Need to disable csrf so that we can use POST via REST
            http.csrf().disable();

            //Need to disable the frame options so we can embed this in another tool
            http.headers().frameOptions().disable();

            http.exceptionHandling().accessDeniedPage("/accessDenied");
        }
    }

    @Configuration
    @Order(2)
    public static class CourseListWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {
        public static final String PATH_TO_SECURE = "/**";

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.authenticationProvider(new LtiAuthenticationProvider());
            http.authorizeRequests(authorizeRequests ->
                  authorizeRequests
                        .antMatchers(PATH_TO_SECURE).hasRole(LtiAuthenticationProvider.LTI_USER)
                        .anyRequest().authenticated());

            //Need to disable csrf so that we can use POST via REST
            http.csrf().disable();

            //Need to disable the frame options so we can embed this in another tool
            http.headers().frameOptions().disable();

            http.exceptionHandling().accessDeniedPage("/accessDenied");
        }

        @Override
        public void configure(WebSecurity web) throws Exception {
            // ignore everything except paths specified
            web.ignoring().antMatchers("/templates/**", "/jsreact/**", "/static/**", "/webjars/**",
                  "/resources/**", "/actuator/**", "/css/**", "/js/**");
        }

    }
}
