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
import edu.iu.uits.lms.canvas.helpers.TermHelper;
import edu.iu.uits.lms.canvas.model.CanvasTerm;
import edu.iu.uits.lms.courselist.model.DecoratedCourse;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@Service
public class EnrollmentClassificationService {

   @Autowired
   private DateService dateService = null;

   public DecoratedCourse.CLASSIFICATION classifyCourse(final DecoratedCourse decoratedCourse) {
      if (isPending(decoratedCourse)) {
         return DecoratedCourse.CLASSIFICATION.PENDING;
      }

      if (isFuture(decoratedCourse)) {
         return DecoratedCourse.CLASSIFICATION.FUTURE;
      }

      if (isCurrent(decoratedCourse)) {
         return DecoratedCourse.CLASSIFICATION.CURRENT;
      }

      if (isPast(decoratedCourse)) {
         return DecoratedCourse.CLASSIFICATION.PAST;
      }

      return DecoratedCourse.CLASSIFICATION.UNKNOWN;
   }

   /**
    * If the user’s enrollments status is “invited”, or “pending”
    *  the course should display in the Pending Enrollments bucket.
    * @param decoratedCourse
    * @return
    */
   private boolean isPending(DecoratedCourse decoratedCourse) {
      String state = decoratedCourse.getEnrollment().getEnrollmentState();
      return EnrollmentHelper.STATE.invited.name().equals(state) || EnrollmentHelper.STATE.creation_pending.name().equals(state);
   }

   /**
    * If the effective start date is in the future
    * AND the user’s enrollment status is “active”,
    * the course should be display in the Future Enrollments Bucket.
    * @param decoratedCourse
    * @return
    */
   private boolean isFuture(DecoratedCourse decoratedCourse) {
      String state = decoratedCourse.getEnrollment().getEnrollmentState();
      Date startDate = getEffectiveStartDate(decoratedCourse);
      return ((EnrollmentHelper.STATE.active.name().equals(state) || EnrollmentHelper.STATE.creation_pending.name().equals(state)) &&
            startDate != null && isFutureDate(startDate));
   }

   /**
    * If the effective end date for the course is empty or in the future
    * AND the effective start date is empty or in the past
    * AND the user’s enrollment status is “active”
    * the course should display in the Current Enrollments bucket.
    * @param decoratedCourse
    * @return
    */
   private boolean isCurrent(DecoratedCourse decoratedCourse) {
      String state = decoratedCourse.getEnrollment().getEnrollmentState();
      Date endDate = getEffectiveEndDate(decoratedCourse);
      Date startDate = getEffectiveStartDate(decoratedCourse);

      return (EnrollmentHelper.STATE.active.name().equals(state)
            && (endDate == null || isFutureDate(endDate))
            && (startDate == null || isPastDate(startDate)));

   }

   /**
    * If the user’s enrollment status is “completed”
    * the course should display in the Past Enrollments bucket, regardless of effective start or end dates.
    * If the effective end date for the course is in the past
    * the course should display in the Past Enrollments bucket, regardless of effective start date.
    * @param decoratedCourse
    * @return
    */
   private boolean isPast(DecoratedCourse decoratedCourse) {
      String state = decoratedCourse.getEnrollment().getEnrollmentState();
      Date endDate = getEffectiveEndDate(decoratedCourse);

      return (EnrollmentHelper.STATE.completed.name().equals(state) || (endDate != null && isPastDate(endDate)));
   }

   /**
    * Override course start date:  Start date set in Course Settings with the “Students can only participate in the course between these dates” box checked (“restrict_enrollments_to_course_dates": true”)
    * Role-specific term start date: the term start date that applies to a specific role (observers use same dates as students)
    * Term Start Date
    * @param decoratedCourse
    * @return
    */
   public Date getEffectiveStartDate(DecoratedCourse decoratedCourse) {
      Date date = TermHelper.getStartDate(decoratedCourse.getTerm());

      String role = typeTranslater(decoratedCourse.getEnrollment().getType());
      Map<String, CanvasTerm.TermOverride> roleOverrides = decoratedCourse.getTerm().getOverrides();
      if (roleOverrides != null && roleOverrides.containsKey(role)) {
         Date roleDate = TermHelper.getStartDate(roleOverrides.get(role));
         if (roleDate != null) {
            date = roleDate;
         }
      }

      if (decoratedCourse.getCourse().isRestrictEnrollmentsToCourseDates()) {
         Date courseDate = CourseHelper.getStartDate(decoratedCourse.getCourse());
         if (courseDate != null) {
            date = courseDate;
         }
      }

      return date;
   }

   public Date getEffectiveEndDate(DecoratedCourse decoratedCourse) {
      Date date = TermHelper.getEndDate(decoratedCourse.getTerm());

      String role = typeTranslater(decoratedCourse.getEnrollment().getType());
      Map<String, CanvasTerm.TermOverride> roleOverrides = decoratedCourse.getTerm().getOverrides();
      if (roleOverrides != null && roleOverrides.containsKey(role)) {
         Date roleDate = TermHelper.getEndDate(roleOverrides.get(role));
         if (roleDate != null) {
            date = roleDate;
         }
      }

      if (decoratedCourse.getCourse().isRestrictEnrollmentsToCourseDates()) {
         Date courseDate = CourseHelper.getEndDate(decoratedCourse.getCourse());
         if (courseDate != null) {
            date = courseDate;
         }
      }

      return date;
   }

   /**
    *
    * @param date
    * @return
    */
   public boolean isFutureDate(@NonNull Date date) {
      Date now = dateService.getCurrentDate();
      return (date.getTime() > now.getTime());
   }

   public boolean isPastDate(@NonNull Date date) {
      Date now = dateService.getCurrentDate();
      return (date.getTime() < now.getTime());
   }

   /**
    * Translate the enrollment type into the base role type
    * @param input
    * @return
    */
   private String typeTranslater(String input) {
      EnrollmentHelper.TYPE type = EnrollmentHelper.TYPE.valueOf(input);
      String roleName;
      switch (type) {
         case teacher:
            roleName = EnrollmentHelper.TYPE_TEACHER;
            break;
         case student:
            roleName = EnrollmentHelper.TYPE_STUDENT;
            break;
         case ta:
            roleName = EnrollmentHelper.TYPE_TA;
            break;
         case designer:
         roleName = EnrollmentHelper.TYPE_DESIGNER;
            break;
         case observer:
            roleName = EnrollmentHelper.TYPE_OBSERVER;
            break;
         default:
            roleName = input;
            break;
      }

      return roleName;
   }
}
