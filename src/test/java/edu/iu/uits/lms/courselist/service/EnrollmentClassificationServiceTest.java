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

import edu.iu.uits.lms.canvas.helpers.CourseHelper;
import edu.iu.uits.lms.canvas.helpers.EnrollmentHelper;
import edu.iu.uits.lms.canvas.model.CanvasTerm;
import edu.iu.uits.lms.canvas.model.Course;
import edu.iu.uits.lms.canvas.model.Enrollment;
import edu.iu.uits.lms.canvas.services.CanvasService;
import edu.iu.uits.lms.canvas.services.CourseService;
import edu.iu.uits.lms.canvas.services.TermService;
import edu.iu.uits.lms.canvas.services.UserService;
import edu.iu.uits.lms.courselist.config.ToolConfig;
import edu.iu.uits.lms.courselist.model.DecoratedCourse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@SpringBootTest(classes = {EnrollmentClassificationService.class, CourseListService.class},
        properties = {"oauth.tokenprovider.url=http://foo"})
@Import({ToolConfig.class})
public class EnrollmentClassificationServiceTest {

   protected CanvasTerm fa19Term;
   protected CanvasTerm su19Term;
   protected CanvasTerm sp19Term;

   protected Course unpublishedCourse;
   protected Course publishedCourse;
   protected Course publishedFutureCourse;
   protected Course publishedPastCourse;

   @MockitoBean
   private DateService dateService = null;

   @Autowired
   private EnrollmentClassificationService enrollmentClassificationService = null;

   @Autowired
   private CourseListService courseListService = null;

   @MockitoBean
   private CourseService courseService = null;

   @MockitoBean
   private TermService termService = null;

   @MockitoBean
   private UserService userService = null;

   @MockitoBean
   private CanvasService canvasService = null;

   @BeforeEach
   public void setUp() throws Exception {
      fa19Term = new CanvasTerm();
      fa19Term.setId("6462");
      fa19Term.setName("Fall 2019");
      fa19Term.setSisTermId("4198");
      fa19Term.setWorkflowState("active");
      fa19Term.setStartAt("2019-07-03T04:00:00Z");
      fa19Term.setEndAt("2019-12-30T05:00:00Z");

      Map<String, CanvasTerm.TermOverride> overridesA = new HashMap<>();
      overridesA.put("StudentEnrollment", newOverride("2019-08-01T04:00:00Z", "2019-12-30T05:00:00Z"));
      overridesA.put("TeacherEnrollment", newOverride("2019-07-03T04:00:00Z", null));
      overridesA.put("TaEnrollment", newOverride("2019-07-03T04:00:00Z", "2019-12-30T05:00:00Z"));
      overridesA.put("DesignerEnrollment", newOverride("2019-06-01T04:00:00Z", "2019-06-27T04:00:00Z"));
      fa19Term.setOverrides(overridesA);

      su19Term = new CanvasTerm();
      su19Term.setId("6461");
      su19Term.setName("Summer 2019");
      su19Term.setSisTermId("4195");
      su19Term.setWorkflowState("active");
      su19Term.setStartAt("2019-04-04T04:00:00Z");
      su19Term.setEndAt("2019-09-22T04:00:00Z");

      sp19Term = new CanvasTerm();
      sp19Term.setId("6425");
      sp19Term.setName("Spring 2019");
      sp19Term.setSisTermId("4192");
      sp19Term.setWorkflowState("active");
      sp19Term.setStartAt("2018-11-01T04:00:00Z");
      sp19Term.setEndAt("2019-06-05T04:00:00Z");

      Calendar cal = Calendar.getInstance();
      cal.set(2019, Calendar.JULY, 1);
      Date currentDate = cal.getTime();
      Mockito.when(dateService.getCurrentDate()).thenReturn(currentDate);

      unpublishedCourse = newCourse(null, null, false, CourseHelper.WORKFLOW_STATE.UNPUBLISHED);
      publishedCourse = newCourse(null, null, false, CourseHelper.WORKFLOW_STATE.AVAILABLE);
      publishedFutureCourse = newCourse("2019-07-03T04:00:00Z", "2020-06-30T04:00:00Z", true, CourseHelper.WORKFLOW_STATE.AVAILABLE);
      publishedPastCourse = newCourse("2019-04-04T04:00:00Z", "2019-06-30T04:00:00Z", true, CourseHelper.WORKFLOW_STATE.AVAILABLE);

   }

   private CanvasTerm.TermOverride newOverride(String start, String end) {
      CanvasTerm.TermOverride override = new CanvasTerm.TermOverride();
      override.setStartAt(start);
      override.setEndAt(end);
      return override;
   }

   private Course newCourse(String startDate, String endDate, boolean restrict, CourseHelper.WORKFLOW_STATE workflowState) {
      Course course = new Course();
      course.setStartAt(startDate);
      course.setEndAt(endDate);
      course.setRestrictEnrollmentsToCourseDates(restrict);
      course.setWorkflowState(workflowState.getText());

      return course;
   }

   private Enrollment newEnrollment(String type, String role, String state) {
      Enrollment enrollment = new Enrollment();
      enrollment.setType(type);
      enrollment.setRole(role);
      enrollment.setEnrollmentState(state);
      return enrollment;
   }

   @Test
   @Disabled
   public void testCourseA() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), EnrollmentHelper.TYPE_TEACHER, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, fa19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
   }

   @Test
   @Disabled
   public void testCourseB() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), "Co-Instructor", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, fa19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
   }

   @Test
   @Disabled
   public void testCourseC() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), "Librarian", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, fa19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
   }

   @Test
   @Disabled
   public void testCourseD() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), EnrollmentHelper.TYPE_TA, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, fa19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
   }

   @Test
   @Disabled
   public void testCourseE() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), "Grader", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, fa19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
   }

   @Test
   public void testCourseF() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.designer.name(), "Designer", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, fa19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);

      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(unpublishedCourse, enrollment));
   }

   @Test
   public void testCourseG() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.observer.name(), EnrollmentHelper.TYPE_OBSERVER, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, fa19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.FUTURE, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertFalse(courseListService.hasClickableLink(unpublishedCourse, enrollment));
   }

   @Test
   public void testCourseH() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.student.name(), EnrollmentHelper.TYPE_STUDENT, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, fa19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.FUTURE, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertFalse(courseListService.hasClickableLink(unpublishedCourse, enrollment));
   }

   @Test
   public void testCourseI() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.student.name(), "Instructor-Added Student", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, fa19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.FUTURE, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertFalse(courseListService.hasClickableLink(unpublishedCourse, enrollment));
   }


   @Test
   @Disabled
   public void testCourseJ() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), EnrollmentHelper.TYPE_TEACHER, EnrollmentHelper.STATE.invited.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, fa19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
   }


   @Test
   public void testCourseK() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.student.name(), EnrollmentHelper.TYPE_STUDENT, EnrollmentHelper.STATE.creation_pending.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, fa19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PENDING, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertFalse(courseListService.hasClickableLink(unpublishedCourse, enrollment));
   }

   @Test
   public void testCourseCompleted() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.student.name(), EnrollmentHelper.TYPE_STUDENT, EnrollmentHelper.STATE.completed.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, fa19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }

   @Test
   public void testCourseL() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), EnrollmentHelper.TYPE_TEACHER, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(unpublishedCourse, enrollment));
   }

   @Test
   public void testCourseM() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), "Co-Instructor", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(unpublishedCourse, enrollment));
   }

   @Test
   public void testCourseN() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), "Librarian", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(unpublishedCourse, enrollment));
   }

   @Test
   public void testCourseO() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), EnrollmentHelper.TYPE_TA, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(unpublishedCourse, enrollment));
   }

   @Test
   public void testCourseP() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), "Grader", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(unpublishedCourse, enrollment));
   }

   @Test
   public void testCourseQ() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.designer.name(), "Designer", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);

      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(unpublishedCourse, enrollment));
   }

   @Test
   public void testCourseR() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.observer.name(), EnrollmentHelper.TYPE_OBSERVER, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertFalse(courseListService.hasClickableLink(unpublishedCourse, enrollment));
   }

   @Test
   public void testCourseS() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.student.name(), EnrollmentHelper.TYPE_STUDENT, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertFalse(courseListService.hasClickableLink(unpublishedCourse, enrollment));
   }

   @Test
   public void testCourseT() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.student.name(), "Instructor-Added Student", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertFalse(courseListService.hasClickableLink(unpublishedCourse, enrollment));
   }


   @Test
   public void testCourseU() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), EnrollmentHelper.TYPE_TEACHER, EnrollmentHelper.STATE.invited.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PENDING, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(unpublishedCourse, enrollment));
   }


   @Test
   public void testCourseV() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.student.name(), EnrollmentHelper.TYPE_STUDENT, EnrollmentHelper.STATE.creation_pending.name());
      DecoratedCourse dc = new DecoratedCourse(unpublishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PENDING, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertFalse(courseListService.hasClickableLink(unpublishedCourse, enrollment));
   }

   @Test
   public void testCourseW() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), EnrollmentHelper.TYPE_TEACHER, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }

   @Test
   public void testCourseX() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), "Co-Instructor", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }

   @Test
   public void testCourseY() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), "Librarian", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }

   @Test
   public void testCourseZ() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), EnrollmentHelper.TYPE_TA, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }

   @Test
   public void testCourseAA() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), "Grader", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }

   @Test
   public void testCourseBB() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.designer.name(), "Designer", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);

      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }

   @Test
   public void testCourseCC() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.observer.name(), EnrollmentHelper.TYPE_OBSERVER, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }

   @Test
   public void testCourseDD() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.student.name(), EnrollmentHelper.TYPE_STUDENT, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }

   @Test
   public void testCourseEE() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.student.name(), "Instructor-Added Student", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.CURRENT, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }


   @Test
   public void testCourseFF() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), EnrollmentHelper.TYPE_TEACHER, EnrollmentHelper.STATE.invited.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PENDING, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }


   @Test
   public void testCourseGG() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.student.name(), EnrollmentHelper.TYPE_STUDENT, EnrollmentHelper.STATE.creation_pending.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PENDING, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }

   @Test
   public void testCourseHH() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), EnrollmentHelper.TYPE_TEACHER, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedFutureCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.FUTURE, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedFutureCourse, enrollment));
   }

   @Test
   public void testCourseII() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), "Co-Instructor", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedFutureCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.FUTURE, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedFutureCourse, enrollment));
   }

   @Test
   public void testCourseJJ() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), "Librarian", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedFutureCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.FUTURE, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedFutureCourse, enrollment));
   }

   @Test
   public void testCourseKK() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), EnrollmentHelper.TYPE_TA, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedFutureCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.FUTURE, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedFutureCourse, enrollment));
   }

   @Test
   public void testCourseLL() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), "Grader", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedFutureCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.FUTURE, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedFutureCourse, enrollment));
   }

   @Test
   public void testCourseMM() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.designer.name(), "Designer", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedFutureCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);

      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.FUTURE, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedFutureCourse, enrollment));
   }

   @Test
   public void testCourseNN() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.observer.name(), EnrollmentHelper.TYPE_OBSERVER, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedFutureCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.FUTURE, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedFutureCourse, enrollment));
   }

   @Test
   public void testCourseOO() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.student.name(), EnrollmentHelper.TYPE_STUDENT, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedFutureCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.FUTURE, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedFutureCourse, enrollment));
   }

   @Test
   public void testCoursePP() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.student.name(), "Instructor-Added Student", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedFutureCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.FUTURE, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedFutureCourse, enrollment));
   }


   @Test
   public void testCourseQQ() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), EnrollmentHelper.TYPE_TEACHER, EnrollmentHelper.STATE.invited.name());
      DecoratedCourse dc = new DecoratedCourse(publishedFutureCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PENDING, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedFutureCourse, enrollment));
   }


   @Test
   public void testCourseRR() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.student.name(), EnrollmentHelper.TYPE_STUDENT, EnrollmentHelper.STATE.creation_pending.name());
      DecoratedCourse dc = new DecoratedCourse(publishedFutureCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PENDING, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedFutureCourse, enrollment));
   }

   @Test
   public void testCourseAB() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), EnrollmentHelper.TYPE_TEACHER, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedPastCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedPastCourse, enrollment));
   }

   @Test
   public void testCourseAC() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), "Co-Instructor", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedPastCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedPastCourse, enrollment));
   }

   @Test
   public void testCourseAD() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), "Librarian", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedPastCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedPastCourse, enrollment));
   }

   @Test
   public void testCourseAE() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), EnrollmentHelper.TYPE_TA, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedPastCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedPastCourse, enrollment));
   }

   @Test
   public void testCourseAF() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), "Grader", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedPastCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedPastCourse, enrollment));
   }

   @Test
   public void testCourseAG() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.designer.name(), "Designer", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedPastCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);

      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedPastCourse, enrollment));
   }

   @Test
   public void testCourseAH() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.observer.name(), EnrollmentHelper.TYPE_OBSERVER, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedPastCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedPastCourse, enrollment));
   }

   @Test
   public void testCourseAI() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.student.name(), EnrollmentHelper.TYPE_STUDENT, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedPastCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedPastCourse, enrollment));
   }

   @Test
   public void testCourseAJ() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.student.name(), "Instructor-Added Student", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedPastCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedPastCourse, enrollment));
   }


   @Test
   public void testCourseAK() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.student.name(), EnrollmentHelper.TYPE_STUDENT, EnrollmentHelper.STATE.completed.name());
      DecoratedCourse dc = new DecoratedCourse(publishedPastCourse, false, false, enrollment, su19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedPastCourse, enrollment));
   }

   @Test
   public void testCourseSP19A() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), EnrollmentHelper.TYPE_TEACHER, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, sp19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }

   @Test
   public void testCourseSP19B() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), "Co-Instructor", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, sp19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }

   @Test
   public void testCourseSP19C() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), "Librarian", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, sp19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }

   @Test
   public void testCourseSP19D() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), EnrollmentHelper.TYPE_TA, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, sp19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }

   @Test
   public void testCourseSP19E() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.teacher.name(), "Grader", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, sp19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }

   @Test
   public void testCourseSP19F() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.designer.name(), "Designer", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, sp19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);

      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }

   @Test
   public void testCourseSP19G() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.observer.name(), EnrollmentHelper.TYPE_OBSERVER, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, sp19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }

   @Test
   public void testCourseSP19H() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.student.name(), EnrollmentHelper.TYPE_STUDENT, EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, sp19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }

   @Test
   public void testCourseSP19I() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.student.name(), "Instructor-Added Student", EnrollmentHelper.STATE.active.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, sp19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }


   @Test
   public void testCourseSP19J() {
      Enrollment enrollment = newEnrollment(EnrollmentHelper.TYPE.student.name(), EnrollmentHelper.TYPE_STUDENT, EnrollmentHelper.STATE.completed.name());
      DecoratedCourse dc = new DecoratedCourse(publishedCourse, false, false, enrollment, sp19Term);
      DecoratedCourse.CLASSIFICATION classification = enrollmentClassificationService.classifyCourse(dc);
      dc.setEnrollmentClassification(classification);
      Assertions.assertEquals(DecoratedCourse.CLASSIFICATION.PAST, dc.getEnrollmentClassification(), "Wrong enrollment classification");
      Assertions.assertTrue(courseListService.hasClickableLink(publishedCourse, enrollment));
   }

   @TestConfiguration
   static class EnrollmentClassificationServiceTestContextConfiguration {
      @Bean
      public EnrollmentClassificationService enrollmentClassificationService() {
         return new EnrollmentClassificationService();
      }

      @Bean
      public CourseListService courseListService() {
         return new CourseListService();
      }
   }
}
