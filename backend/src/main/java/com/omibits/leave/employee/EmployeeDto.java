package com.omibits.leave.employee;

import com.omibits.leave.common.InputRules;
import jakarta.validation.constraints.*;
import java.time.Instant;

public final class EmployeeDto {

  private EmployeeDto() {}

  public record Create(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 120, message = "Name must be 2–120 characters")
    @Pattern(
      regexp = InputRules.PERSON_NAME,
      message = "Use letters, spaces, dots, apostrophes or hyphens only"
    )
    String fullName,
    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 190, message = "Email must be 190 characters or fewer")
    String email,
    @NotNull(message = "Select a gender") UserAccount.Gender gender,
    @NotNull(message = "Select a department")
    @Positive(message = "Select a valid department")
    Long departmentId,
    @NotBlank(message = "Password is required")
    @Pattern(
      regexp = InputRules.STRONG_PASSWORD,
      message = "Use 8–72 characters with uppercase, lowercase, number and symbol; no spaces"
    )
    String password
  ) {}

  public record Update(
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 120, message = "Name must be 2–120 characters")
    @Pattern(
      regexp = InputRules.PERSON_NAME,
      message = "Use letters, spaces, dots, apostrophes or hyphens only"
    )
    String fullName,
    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 190, message = "Email must be 190 characters or fewer")
    String email,
    @NotNull(message = "Select a gender") UserAccount.Gender gender,
    @NotNull(message = "Select a department")
    @Positive(message = "Select a valid department")
    Long departmentId,
    @NotNull(message = "Select an account status") Boolean active
  ) {}

  public record View(
    Long id,
    String fullName,
    String email,
    UserAccount.Gender gender,
    UserAccount.Role role,
    Long departmentId,
    String department,
    boolean active,
    Instant createdAt
  ) {
    public static View from(UserAccount u) {
      var department = u.getDepartment();
      return new View(
        u.getId(),
        u.getFullName(),
        u.getEmail(),
        u.getGender(),
        u.getRole(),
        department == null ? null : department.getId(),
        department == null ? null : department.getName(),
        u.isActive(),
        u.getCreatedAt()
      );
    }
  }
}
