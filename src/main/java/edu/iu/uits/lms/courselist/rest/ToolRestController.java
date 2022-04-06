package edu.iu.uits.lms.courselist.rest;

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

import edu.iu.uits.lms.canvas.model.Favorite;
import edu.iu.uits.lms.courselist.controller.CourselistController;
import edu.iu.uits.lms.courselist.model.DecoratedCourse;
import edu.iu.uits.lms.courselist.service.CourseListService;
import edu.iu.uits.lms.lti.service.OidcTokenUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ox.ctl.lti13.security.oauth2.client.lti.authentication.OidcAuthenticationToken;

import java.util.List;

@RestController
@RequestMapping("/app")
@Slf4j
public class ToolRestController extends CourselistController {

   @Autowired
   private CourseListService courseListService = null;

   @GetMapping("/courses")
   public List<DecoratedCourse> getCourses() {
      log.debug("in /app/courses");
      OidcAuthenticationToken token = getTokenWithoutContext();
      OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);

      return courseListService.getCourses(oidcTokenUtils.getUserLoginId());
   }

   @PostMapping("/hide/{courseId}")
   public ReturnState hideCourse(@PathVariable String courseId) {
      log.debug("in /app/hide/{}", courseId);
      OidcAuthenticationToken token = getTokenWithoutContext();
      OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);

      boolean success = courseListService.setCourseAsHidden(oidcTokenUtils.getUserLoginId(), courseId);
      return new ReturnState(success, null);
   }

   @PostMapping("/show/{courseId}")
   public ReturnState showCourse(@PathVariable String courseId) {
      log.debug("in /app/show/{}", courseId);
      OidcAuthenticationToken token = getTokenWithoutContext();
      OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);

      boolean success = courseListService.setCourseAsShown(oidcTokenUtils.getUserLoginId(), courseId);
      return new ReturnState(!success, null);
   }

   @PostMapping("/favorite/{courseId}")
   public ReturnState favoriteCourse(@PathVariable String courseId) {
      log.debug("in /app/favorite/{}", courseId);
      OidcAuthenticationToken token = getTokenWithoutContext();
      OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);

      Favorite favorite = courseListService.setCourseAsFavorite(oidcTokenUtils.getUserLoginId(), courseId);
      boolean success = favorite != null && courseId.equals(favorite.getContextId());
      return new ReturnState(null, success);
   }

   @PostMapping("/unfavorite/{courseId}")
   public ReturnState unfavoriteCourse(@PathVariable String courseId) {
      log.debug("in /app/unfavorite/{}", courseId);
      OidcAuthenticationToken token = getTokenWithoutContext();
      OidcTokenUtils oidcTokenUtils = new OidcTokenUtils(token);

      Favorite favorite = courseListService.removeCourseAsFavorite(oidcTokenUtils.getUserLoginId(), courseId);
      boolean success = favorite != null && courseId.equals(favorite.getContextId());
      return new ReturnState(null, !success);
   }

   @Data
   @AllArgsConstructor
   public static class ReturnState {
      private Boolean hidden;
      private Boolean favorited;
   }

}
