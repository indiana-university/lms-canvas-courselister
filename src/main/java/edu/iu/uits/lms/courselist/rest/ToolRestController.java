package edu.iu.uits.lms.courselist.rest;

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
