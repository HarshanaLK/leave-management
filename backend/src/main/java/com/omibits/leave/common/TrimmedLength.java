package com.omibits.leave.common;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Validates the character count after removing surrounding whitespace. */
@Target({
  ElementType.FIELD,
  ElementType.PARAMETER,
  ElementType.RECORD_COMPONENT,
  ElementType.ANNOTATION_TYPE,
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = TrimmedLengthValidator.class)
public @interface TrimmedLength {
  String message() default "Must contain {min}–{max} characters excluding surrounding whitespace";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  int min() default 0;

  int max() default Integer.MAX_VALUE;
}
