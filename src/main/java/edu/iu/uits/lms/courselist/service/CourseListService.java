package edu.iu.uits.lms.courselist.service;

import canvas.client.generated.api.CanvasApi;
import canvas.client.generated.api.CoursesApi;
import canvas.client.generated.api.TermsApi;
import canvas.client.generated.api.UsersApi;
import canvas.client.generated.model.CanvasTerm;
import canvas.client.generated.model.Course;
import canvas.client.generated.model.Enrollment;
import canvas.client.generated.model.Favorite;
import canvas.client.generated.model.UserCustomDataRequest;
import canvas.helpers.CanvasConstants;
import canvas.helpers.CourseHelper;
import canvas.helpers.EnrollmentHelper;
import canvas.helpers.TermHelper;
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
   private CoursesApi coursesApi = null;

   @Autowired
   private TermsApi termsApi = null;

   @Autowired
   private UsersApi usersApi = null;

   @Autowired
   private CanvasApi canvasApi = null;

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
      List<Course> courses = coursesApi.getCoursesForUser(userLoginId, false, false,
            false, workflowStates);

      Set<String> hidden = getHiddenCourseIds(userLoginId);
      return decorateCourses(courses, hidden);
   }

   public String getCanvasBaseUrl() {
      return canvasApi.getBaseUrl();
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
                  DecoratedCourse dc = new DecoratedCourse(course, course.getIsFavorite(), hidden.contains(course.getId()),
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
                  Date dateToOrder = TermHelper.getEndDate(term);
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
      List<CanvasTerm> terms =  termsApi.getEnrollmentTerms();
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
         Map<String, Map<String, String>> data = (Map) usersApi.getUserCustomData(customDataRequest);

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
      Object results = usersApi.setUserCustomData(customDataRequest);
      return results != null;
   }

   public boolean setCourseAsShown(String asUserLogin, String courseId) {
      List<String> pathParts = Arrays.asList(CUSTOM_USER_DATA_KEY, courseId);
      UserCustomDataRequest customDataRequest = new UserCustomDataRequest();
      customDataRequest.setUserId(asUserLogin);
      customDataRequest.setField(CanvasConstants.API_FIELD_SIS_LOGIN_ID);
      customDataRequest.setPathParts(pathParts);
      Object results = usersApi.deleteUserCustomData(customDataRequest);
      return results != null;
   }

   public Favorite setCourseAsFavorite(String asUserLogin, String courseId) {
      return coursesApi.addCourseToFavorites(asUserLogin, courseId);
   }

   public Favorite removeCourseAsFavorite(String asUserLogin, String courseId) {
      return coursesApi.removeCourseAsFavorite(asUserLogin, courseId);
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
