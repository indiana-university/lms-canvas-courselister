package edu.iu.uits.lms.courselist.model;

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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import edu.iu.uits.lms.canvas.model.CanvasTerm;
import edu.iu.uits.lms.canvas.model.Course;
import edu.iu.uits.lms.canvas.model.Enrollment;
import edu.iu.uits.lms.courselist.service.EnrollmentClassificationSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class DecoratedCourse {

   @NonNull
   private Course course;

   @NonNull
   private boolean isFavorited;

   private boolean isFavoritable;

   private boolean isPublished;

   @NonNull
   private boolean isHidden;

   @NonNull
   private Enrollment enrollment;

   @NonNull
   private CanvasTerm term;

   private CLASSIFICATION enrollmentClassification;

   private String courseName;

   private String courseNickName;

   private String termSort;

   private String roleLabel;

   private String baseRoleLabel;

   private boolean isLinkClickable;

   private String courseCode = "";


   @AllArgsConstructor
   @Getter
   @JsonSerialize(using = EnrollmentClassificationSerializer.class)
   public enum CLASSIFICATION {
      PAST("Past Enrollments", 3),
      CURRENT("Current Enrollments", 1),
      FUTURE("Future Enrollments", 2),
      PENDING("Pending Enrollments", 0),
      UNKNOWN("Other Enrollments", 4);

      private String text;
      private int order;

   }

}
