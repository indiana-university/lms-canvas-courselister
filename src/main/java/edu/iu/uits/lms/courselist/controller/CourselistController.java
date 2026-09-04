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

import edu.iu.uits.lms.canvasoauth2.CanvasOAuth2Registration;
import edu.iu.uits.lms.courselist.config.ToolConfig;
import edu.iu.uits.lms.courselist.service.CourseListService;
import edu.iu.uits.lms.lti.LTIConstants;
import edu.iu.uits.lms.lti.controller.OidcTokenAwareController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.ClientAuthorizationRequiredException;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@Slf4j
public class CourselistController extends OidcTokenAwareController {

    @Autowired
    private ToolConfig toolConfig = null;

    @Autowired
    private CourseListService courseListService = null;

    @Autowired
    private OAuth2AuthorizedClientRepository canvasOAuth2AuthorizedClientRepository = null;

    @Autowired
    private CanvasOAuth2Registration canvasOAuth2Registration = null;

    @RequestMapping("/list")
    @Secured(LTIConstants.BASE_USER_AUTHORITY)
    public ModelAndView list(Model model, HttpSession httpSession, SecurityContextHolderAwareRequestWrapper request) {
        log.debug("in /list");
        getTokenWithoutContext();

        // Course listing, favoriting, and hide/show all run as the launching user's own Canvas OAuth2
        // token now (see CourseListService) - require that token to exist before rendering the SPA
        // shell, since the REST endpoints it calls afterward return JSON and have no way to render
        // the HTML consent breakout page themselves.
        ensureCanvasOAuth2Consent(SecurityContextHolder.getContext().getAuthentication(), request);

        String canvasBaseUrl = courseListService.getCanvasBaseUrl();
        model.addAttribute("browseCoursesUrl", canvasBaseUrl + "/search/all_courses/");
        model.addAttribute("siteRequestUrl", canvasBaseUrl + toolConfig.getStartANewCourseUrl());
        model.addAttribute("canvasBaseUrl", canvasBaseUrl);

        //For session tracking
        model.addAttribute("customId", httpSession.getId());
        return new ModelAndView("react");
    }

    /**
     * Makes sure the current user has an authorized Canvas OAuth2 client on file before letting the
     * course-list page render (every per-user Canvas call it and its REST endpoints make depends on
     * it). If no authorized client exists yet, throws the same exception the vendored
     * {@code @RegisteredOAuth2AuthorizedClient} resolver would have thrown;
     * {@code OAuth2ConsentControllerAdvice} catches it and renders the "connect your Canvas account"
     * consent breakout page instead of an error.
     * @param principal the current authentication
     * @param request the current request
     */
    private void ensureCanvasOAuth2Consent(Authentication principal, HttpServletRequest request) {
        OAuth2AuthorizedClient authorizedClient = canvasOAuth2AuthorizedClientRepository
                .loadAuthorizedClient(canvasOAuth2Registration.getRegistrationId(), principal, request);
        if (authorizedClient == null) {
            throw new ClientAuthorizationRequiredException(canvasOAuth2Registration.getRegistrationId());
        }
    }
}
