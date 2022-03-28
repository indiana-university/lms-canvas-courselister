package edu.iu.uits.lms.courselist.model;

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
