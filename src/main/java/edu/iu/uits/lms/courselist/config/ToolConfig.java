package edu.iu.uits.lms.courselist.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "courselist")
@Getter
@Setter
public class ToolConfig {
   private String startANewCourseUrl;
   private String version;
   private String env;
}
