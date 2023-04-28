package edu.iu.uits.lms.courselist.config;

/*-
 * #%L
 * lms-lti-courselist
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import edu.iu.uits.lms.common.it12logging.LmsFilterSecurityInterceptorObjectPostProcessor;
import edu.iu.uits.lms.lti.service.LmsDefaultGrantedAuthoritiesMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import uk.ac.ox.ctl.lti13.Lti13Configurer;

import static edu.iu.uits.lms.lti.LTIConstants.BASE_USER_ROLE;
import static edu.iu.uits.lms.lti.LTIConstants.WELL_KNOWN_ALL;

@Configuration
@Slf4j
@EnableWebSecurity
public class SecurityConfig {

    @Configuration
    @Order(SecurityProperties.BASIC_AUTH_ORDER - 3)
    public static class CourseListWebSecurityConfigurerAdapter extends WebSecurityConfigurerAdapter {

        @Autowired
        private LmsDefaultGrantedAuthoritiesMapper lmsDefaultGrantedAuthoritiesMapper;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                    .requestMatchers()
                    .and()
                    .authorizeRequests()
                    .antMatchers(WELL_KNOWN_ALL, "/error").permitAll()
                    .antMatchers("/**").hasRole(BASE_USER_ROLE)
                    .withObjectPostProcessor(new LmsFilterSecurityInterceptorObjectPostProcessor())
                    .and()
                    .headers()
                    .contentSecurityPolicy("form-action 'self'; frame-ancestors 'self' https://*.instructure.com")
                    .and()
                    .referrerPolicy(referrer -> referrer
                            .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN));

            //Setup the LTI handshake
            Lti13Configurer lti13Configurer = new Lti13Configurer()
                    .grantedAuthoritiesMapper(lmsDefaultGrantedAuthoritiesMapper);

            http.apply(lti13Configurer);

            http.exceptionHandling().accessDeniedPage("/accessDenied");
        }

        @Override
        public void configure(WebSecurity web) throws Exception {
            // ignore everything except paths specified
            web.ignoring().antMatchers("/templates/**", "/jsreact/**", "/static/**", "/webjars/**",
                  "/resources/**", "/css/**", "/js/**", "/jsrivet/**");
        }

    }
}