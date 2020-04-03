package edu.iu.uits.lms.courselist.rest;

import canvas.client.generated.model.Favorite;
import edu.iu.uits.lms.courselist.controller.CourselistController;
import edu.iu.uits.lms.courselist.model.DecoratedCourse;
import edu.iu.uits.lms.courselist.service.CourseListService;
import edu.iu.uits.lms.lti.security.LtiAuthenticationToken;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/app")
@Slf4j
public class ToolRestController extends CourselistController {

   @Autowired
   private CourseListService courseListService = null;

   @GetMapping("/courses")
   public List<DecoratedCourse> getCourses() {
      LtiAuthenticationToken token = getTokenWithoutContext();

      return courseListService.getCourses((String)token.getPrincipal());
   }

   @PostMapping("/hide/{courseId}")
   public ReturnState hideCourse(@PathVariable String courseId) {
      LtiAuthenticationToken token = getTokenWithoutContext();
      boolean success = courseListService.setCourseAsHidden((String)token.getPrincipal(), courseId);
      return new ReturnState(success, null);
   }

   @PostMapping("/show/{courseId}")
   public ReturnState showCourse(@PathVariable String courseId) {
      LtiAuthenticationToken token = getTokenWithoutContext();
      boolean success = courseListService.setCourseAsShown((String)token.getPrincipal(), courseId);
      return new ReturnState(!success, null);
   }

   @PostMapping("/favorite/{courseId}")
   public ReturnState favoriteCourse(@PathVariable String courseId) {
      LtiAuthenticationToken token = getTokenWithoutContext();
      Favorite favorite = courseListService.setCourseAsFavorite((String)token.getPrincipal(), courseId);
      boolean success = favorite != null && courseId.equals(favorite.getContextId());
      return new ReturnState(null, success);
   }

   @PostMapping("/unfavorite/{courseId}")
   public ReturnState unfavoriteCourse(@PathVariable String courseId) {
      LtiAuthenticationToken token = getTokenWithoutContext();
      Favorite favorite = courseListService.removeCourseAsFavorite((String)token.getPrincipal(), courseId);
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
