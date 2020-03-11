package edu.iu.uits.lms.courselist.service;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import edu.iu.uits.lms.courselist.model.DecoratedCourse;

import java.io.IOException;

public class EnrollmentClassificationSerializer extends StdSerializer<DecoratedCourse.CLASSIFICATION> {

   public EnrollmentClassificationSerializer() {
      super(DecoratedCourse.CLASSIFICATION.class);
   }

   public EnrollmentClassificationSerializer(Class t) {
      super(t);
   }

   public void serialize(DecoratedCourse.CLASSIFICATION classification, JsonGenerator generator,
                         SerializerProvider provider)
         throws IOException {
      generator.writeStartObject();
      generator.writeFieldName("name");
      generator.writeString(classification.name());
      generator.writeFieldName("text");
      generator.writeString(classification.getText());
      generator.writeFieldName("order");
      generator.writeNumber(classification.getOrder());
      generator.writeEndObject();
   }
}
