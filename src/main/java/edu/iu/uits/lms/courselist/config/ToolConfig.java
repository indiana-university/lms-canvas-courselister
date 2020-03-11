package edu.iu.uits.lms.courselist.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@ConfigurationProperties(prefix = "courselist")
@PropertySource(value = {"classpath:env.properties",
      "classpath:default.properties",
      "classpath:application.properties",
      "${app.fullFilePath}/lms.properties",
      "${app.fullFilePath}/protected.properties",
      "${app.fullFilePath}/security.properties"}, ignoreResourceNotFound = true)


@Getter
@Setter
public class ToolConfig {
   private String canvasBaseUrl;
   private String startANewCourseUrl;
   private String version;
}
