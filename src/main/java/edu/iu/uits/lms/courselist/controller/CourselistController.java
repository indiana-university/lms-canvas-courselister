package edu.iu.uits.lms.courselist.controller;

import edu.iu.uits.lms.courselist.config.ToolConfig;
import edu.iu.uits.lms.courselist.service.CourseListService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.controller.OidcTokenAwareController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpSession;

@Controller
@Slf4j
public class CourselistController extends OidcTokenAwareController {

    @Autowired
    private ToolConfig toolConfig = null;

    @Autowired
    private CourseListService courseListService = null;

    @RequestMapping("/list")
    @Secured(LTIConstants.BASE_USER_AUTHORITY)
    public ModelAndView list(Model model, HttpSession httpSession) {
        log.debug("in /list");
        getTokenWithoutContext();
        String canvasBaseUrl = courseListService.getCanvasBaseUrl();
        model.addAttribute("browseCoursesUrl", canvasBaseUrl + "/search/all_courses/");
        model.addAttribute("siteRequestUrl", canvasBaseUrl + toolConfig.getStartANewCourseUrl());
        model.addAttribute("canvasBaseUrl", canvasBaseUrl);

        //For session tracking
        model.addAttribute("customId", httpSession.getId());
        return new ModelAndView("react");
    }

    @RequestMapping(value = "/accessDenied")
    public String accessDenied() {
        return "accessDenied";
    }
}
