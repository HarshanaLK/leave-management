package com.omibits.leave.common;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Mirrors the .strip() validation enforced by LeaveService. */
public final class TrimmedLengthValidator
  implements ConstraintValidator<TrimmedLength, String>
{

  private int min;
  private int max;

  @Override
  public void initialize(TrimmedLength constraint) {
    this.min = constraint.min();
    this.max = constraint.max();
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    // Leave @NotBlank to report missing values separately.
    if (value == null) return true;
    int length = value.strip().length();
    return length >= min && length <= max;
  }
}
