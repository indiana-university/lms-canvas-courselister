package edu.iu.uits.lms.courselist.service;

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

import edu.iu.uits.lms.canvas.helpers.CanvasConstants;
import edu.iu.uits.lms.canvas.helpers.CourseHelper;
import edu.iu.uits.lms.canvas.helpers.EnrollmentHelper;
import edu.iu.uits.lms.canvas.helpers.TermHelper;
import edu.iu.uits.lms.canvas.model.CanvasTerm;
import edu.iu.uits.lms.canvas.model.Course;
import edu.iu.uits.lms.canvas.model.Enrollment;
import edu.iu.uits.lms.canvas.model.Favorite;
import edu.iu.uits.lms.canvas.model.UserCustomDataRequest;
import edu.iu.uits.lms.canvas.services.CanvasService;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.canvas.services.TermService;
import edu.iu.uits.lms.canvas.services.UserService;
import edu.iu.uits.lms.courselist.model.DecoratedCourse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CourseListService {

   private static final String CUSTOM_USER_DATA_KEY = "hidden_courses";

   @Autowired
   private CourseService courseService = null;

   @Autowired
   private TermService termService = null;

   @Autowired
   private UserService userService = null;

   @Autowired
   private CanvasService canvasService = null;

   @Autowired
   private EnrollmentClassificationService enrollmentClassificationService = null;

   public List<DecoratedCourse> getCourses(String userLoginId) {
      //List<Course> getCoursesForUser(String iuNetworkId, boolean includeSections, boolean includeTerm, boolean excludeBlueprint,
      //            List<Course.WORKFLOW_STATE> states, List<Enrollment.STATE> enrollmentStates)

      //courses?as_user_id=sis_login_id:username&
      // state[]=unpublished, available&
      // enrollment_state[]=active,invited_or_pending,completed

      List<String> workflowStates = Arrays.asList(CourseHelper.WORKFLOW_STATE.AVAILABLE.getText(),
            CourseHelper.WORKFLOW_STATE.UNPUBLISHED.getText(), CourseHelper.WORKFLOW_STATE.COMPLETED.getText());

      //Not including the term since it won't have the overrides in it.
      List<Course> courses = courseService.getCoursesForUser(userLoginId, false, false,
            false, workflowStates);

      Set<String> hidden = getHiddenCourseIds(userLoginId);
      return decorateCourses(courses, hidden);
   }

   public String getCanvasBaseUrl() {
      return canvasService.getBaseUrl();
   }

   /**
    *
    * @param courses
    * @param hidden
    * @return
    */
   private List<DecoratedCourse> decorateCourses(List<Course> courses, Set<String> hidden) {
      List<DecoratedCourse> decoratedCourses = new ArrayList<>();

      Map<String, CanvasTerm> termMap = getTerms();

      for (Course course : courses) {
         List<Enrollment> enrollments = course.getEnrollments();

         //Sometimes enrollments are null instead of empty
         if (enrollments != null) {
            Set<String> seenRoles = new HashSet<>();

            for (Enrollment enrollment : enrollments) {
               //In case the user is in more than one section with the same role, we don't need to see multiples of that
               if (!seenRoles.contains(enrollment.getRole())) {
                  CanvasTerm term = termMap.get(course.getEnrollmentTermId());
                  DecoratedCourse dc = new DecoratedCourse(course, course.isFavorite(), hidden.contains(course.getId()),
                        enrollment, term);

                  dc.setPublished(CourseHelper.isPublished(course));

                  DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
                  if (classification.equals(DecoratedCourse.CLASSIFICATION.PENDING)) {
                     // if it's pending and we're past the end date, don't render it
                     Date endDate = enrollmentClassificationService.getEffectiveEndDate(dc);
                     if (endDate != null && enrollmentClassificationService.isPastDate(endDate)) {
                        break;
                     }
                  }
                  dc.setEnrollmentClassification(classification);

                  dc.setCourseName(course.getName());

                  //If original name is not null, it means the user set a nickname
                  if (course.getOriginalName() != null) {
                     dc.setCourseName(course.getOriginalName());
                     dc.setCourseNickName(course.getName());
                  }

                  String courseCode = course.getCourseCode();
                  if (courseCode != null) {
                     dc.setCourseCode(courseCode);
                  }

                  //Figure out how a term would sort
                  Date dateToOrder = TermHelper.getStartDate(term);
                  long orderTime = dateToOrder != null ? dateToOrder.getTime() : 0;
                  dc.setTermSort(Long.toString(orderTime));

                  //Should the course url be rendered
                  dc.setLinkClickable(hasClickableLink(course, enrollment));

                  //Can the course be favorited?
                  // the linkClickable logic follows the same rules to determine if a student/observer should be able
                  // to favorite a course, so just use it!
                  dc.setFavoritable((DecoratedCourse.CLASSIFICATION.FUTURE.equals(classification) ||
                          DecoratedCourse.CLASSIFICATION.CURRENT.equals(classification)) &&
                          dc.isLinkClickable());

                  //Better labels for the roles
                  dc.setRoleLabel(roleTranslater(enrollment.getRole()));
                  dc.setBaseRoleLabel(typeTranslater(EnrollmentHelper.TYPE.valueOf(enrollment.getType())));

                  decoratedCourses.add(dc);
                  seenRoles.add(enrollment.getRole());
               }
            }
         }
      }

      return decoratedCourses;
   }

   public boolean hasClickableLink(Course course, Enrollment enrollment) {
      final List<String> rolesForUrls = Arrays.asList("teacher", "ta", "designer");
      return CourseHelper.isPublished(course) || rolesForUrls.contains(enrollment.getType());
   }

   private Map<String, CanvasTerm> getTerms() {
      List<CanvasTerm> terms =  termService.getEnrollmentTerms();
      return terms.stream().collect(Collectors.toMap(CanvasTerm::getId, Function.identity()));
   }

   private Set<String> getHiddenCourseIds(String asUserLogin) {
      Set<String> hiddenCourseIds = new HashSet<>();
      List<String> pathParts = Collections.singletonList(CUSTOM_USER_DATA_KEY);
      UserCustomDataRequest customDataRequest = new UserCustomDataRequest();
      customDataRequest.setUserId(asUserLogin);
      customDataRequest.setField(CanvasConstants.API_FIELD_SIS_LOGIN_ID);
      customDataRequest.setPathParts(pathParts);
      try {
         Map<String, Map<String, String>> data = (Map) userService.getUserCustomData(customDataRequest);

         if (data != null) {
            hiddenCourseIds = data.get("data").keySet();
         }
      } catch (HttpClientErrorException hcee) {
         log.warn("no data found");
      }
      return hiddenCourseIds;
   }

   public boolean setCourseAsHidden(String asUserLogin, String courseId) {
      List<String> pathParts = Arrays.asList(CUSTOM_USER_DATA_KEY, courseId);
      UserCustomDataRequest customDataRequest = new UserCustomDataRequest();
      customDataRequest.setUserId(asUserLogin);
      customDataRequest.setField(CanvasConstants.API_FIELD_SIS_LOGIN_ID);
      customDataRequest.setPathParts(pathParts);
      customDataRequest.setData(courseId);
      Object results = userService.setUserCustomData(customDataRequest);
      return results != null;
   }

   public boolean setCourseAsShown(String asUserLogin, String courseId) {
      List<String> pathParts = Arrays.asList(CUSTOM_USER_DATA_KEY, courseId);
      UserCustomDataRequest customDataRequest = new UserCustomDataRequest();
      customDataRequest.setUserId(asUserLogin);
      customDataRequest.setField(CanvasConstants.API_FIELD_SIS_LOGIN_ID);
      customDataRequest.setPathParts(pathParts);
      Object results = userService.deleteUserCustomData(customDataRequest);
      return results != null;
   }

   public Favorite setCourseAsFavorite(String asUserLogin, String courseId) {
      return courseService.addCourseToFavorites(asUserLogin, courseId);
   }

   public Favorite removeCourseAsFavorite(String asUserLogin, String courseId) {
      return courseService.removeCourseAsFavorite(asUserLogin, courseId);
   }

   /**
    * Translate the enrollment role into a display value
    * @param input
    * @return
    */
   private String roleTranslater(String input) {
      String roleName;
      switch (input) {
         case EnrollmentHelper.TYPE_TEACHER:
            roleName = "Teacher";
            break;
         case EnrollmentHelper.TYPE_STUDENT:
            roleName = "Student";
            break;
         case EnrollmentHelper.TYPE_TA:
            roleName = "TA";
            break;
         case EnrollmentHelper.TYPE_DESIGNER:
            roleName = "Designer";
            break;
         case EnrollmentHelper.TYPE_OBSERVER:
            roleName = "Observer";
            break;
         default:
            roleName = input;
            break;
      }

      return roleName;
   }

   /**
    * Translate the enrollment type into a display value
    * @param input
    * @return
    */
   private String typeTranslater(EnrollmentHelper.TYPE input) {
      String roleName;
      switch (input) {
         case teacher:
            roleName = "Teacher";
            break;
         case student:
            roleName = "Student";
            break;
         case ta:
            roleName = "TA";
            break;
         case designer:
            roleName = "Designer";
            break;
         case observer:
            roleName = "Observer";
            break;
         default:
            roleName = input.name();
            break;
      }

      return roleName;
   }
}
