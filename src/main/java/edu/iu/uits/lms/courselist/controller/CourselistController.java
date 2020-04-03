package edu.iu.uits.lms.courselist.controller;

import edu.iu.uits.lms.courselist.config.ToolConfig;
import edu.iu.uits.lms.lti.controller.LtiAuthenticationTokenAwareController;
import edu.iu.uits.lms.lti.security.LtiAuthenticationProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@Slf4j
public class CourselistController extends LtiAuthenticationTokenAwareController {

    @Autowired
    private ToolConfig toolConfig = null;

    @RequestMapping("/list")
    @Secured(LtiAuthenticationProvider.LTI_USER_ROLE)
    public ModelAndView list(Model model, @CookieValue(name = "lmssession", required = false) String cookie) {
        getTokenWithoutContext();
        model.addAttribute("browseCoursesUrl", toolConfig.getCanvasBaseUrl() + "/search/all_courses/");
        model.addAttribute("siteRequestUrl", toolConfig.getCanvasBaseUrl() + toolConfig.getStartANewCourseUrl());
        model.addAttribute("canvasBaseUrl", toolConfig.getCanvasBaseUrl());
        return new ModelAndView("react");
    }

    @RequestMapping(value = "/accessDenied")
    public String accessDenied() {
        return "accessDenied";
    }
}
