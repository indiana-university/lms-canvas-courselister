package edu.iu.uits.lms.courselist;

import edu.iu.uits.lms.common.samesite.CookieFilterConfig;
import edu.iu.uits.lms.common.server.GitRepositoryState;
import edu.iu.uits.lms.common.server.ServerInfo;
import edu.iu.uits.lms.common.server.ServerUtils;
import edu.iu.uits.lms.courselist.config.ToolConfig;
import edu.iu.uits.lms.redis.config.RedisConfiguration;
import lombok.extern.log4j.Log4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

import java.util.Date;

@SpringBootApplication
@PropertySource(value = {"classpath:env.properties",
      "classpath:default.properties",
      "${app.fullFilePath}/lms.properties",
      "${app.fullFilePath}/protected.properties",
      "${app.fullFilePath}/security.properties"}, ignoreResourceNotFound = true)
@EnableResourceServer
@Log4j
@Import({GitRepositoryState.class, RedisConfiguration.class, CookieFilterConfig.class})
public class WebApplication {

    @Autowired
    private ToolConfig toolConfig;

    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }

    @Autowired
    private GitRepositoryState gitRepositoryState;

    @Bean(name = ServerInfo.BEAN_NAME)
    ServerInfo serverInfo() {
        ServerInfo serverInfo = new ServerInfo();
//        serverInfo.setServerUrl(filePropertiesService.getLmsHost());
        serverInfo.setServerName(ServerUtils.getServerHostName());
        serverInfo.setBuildDate(new Date());
        serverInfo.setGitInfo(gitRepositoryState.getBranch() + "@" + gitRepositoryState.getCommitIdAbbrev());
        serverInfo.setArtifactVersion(toolConfig.getVersion());
        return serverInfo;
    }

}
