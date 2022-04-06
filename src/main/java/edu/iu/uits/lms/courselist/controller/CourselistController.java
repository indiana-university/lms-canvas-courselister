package edu.iu.uits.lms.courselist.controller;

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
