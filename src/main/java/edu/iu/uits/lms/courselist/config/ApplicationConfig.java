package edu.iu.uits.lms.courselist.config;

import canvas.config.CanvasClientConfig;
import edu.iu.uits.lms.lti.config.LtiClientConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.session.web.http.HeaderHttpSessionIdResolver;
import org.springframework.session.web.http.HttpSessionIdResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableWebMvc
@EnableGlobalMethodSecurity(securedEnabled = true)
@Slf4j
@Import({LtiClientConfig.class, CanvasClientConfig.class})
public class ApplicationConfig implements WebMvcConfigurer {

   public ApplicationConfig() {
      log.debug("ApplicationConfig()");
   }

   @Override
   // used to read in various directories to add resources for the templates to use
   public void addResourceHandlers(ResourceHandlerRegistry registry) {
      registry.addResourceHandler("/css/**").addResourceLocations("classpath:/static/css/");
      registry.addResourceHandler("/webjars/**").addResourceLocations("/webjars/").resourceChain(true);
      registry.addResourceHandler("/jsreact/**").addResourceLocations("classpath:/META-INF/resources/jsreact/").resourceChain(true);
   }

   /**
    * Uses an x-auth-token header value instead of a cookie for tracking the session
    */
   @Bean
   public HttpSessionIdResolver httpSessionIdResolver() {
      return HeaderHttpSessionIdResolver.xAuthToken();
   }
}
