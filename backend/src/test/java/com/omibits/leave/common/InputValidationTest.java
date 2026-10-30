package com.omibits.leave.common;

import static org.junit.jupiter.api.Assertions.*;

import com.omibits.leave.auth.AuthController;
import com.omibits.leave.department.DepartmentController;
import com.omibits.leave.employee.EmployeeDto;
import com.omibits.leave.employee.UserAccount;
import com.omibits.leave.leave.LeaveDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InputValidationTest {

  private final Validator v =
    Validation.buildDefaultValidatorFactory().getValidator();

  private EmployeeDto.Create employee(
    String name,
    String email,
    String password
  ) {
    return new EmployeeDto.Create(
      name,
      email,
      UserAccount.Gender.OTHER,
      1L,
      password
    );
  }

  @Test
  void acceptsInternationalNameAndStrongPassword() {
    assertTrue(
      v
        .validate(employee("José Silva", "jose@example.org", "Strong#123"))
        .isEmpty()
    );
  }

  @Test
  void rejectsInvalidEmployeeFields() {
    assertFalse(v.validate(employee("J", "not-an-email", "weak")).isEmpty());
    assertFalse(
      v.validate(employee("John123", "john@example.org", "Good#123")).isEmpty()
    );
    assertFalse(
      v
        .validate(employee("Jane Smith", "jane@example.org", "password123"))
        .isEmpty()
    );
  }

  @Test
  void rejectsEmptyDepartmentAndOverlengthDepartment() {
    assertFalse(v.validate(new DepartmentController.Create(" ")).isEmpty());
    assertFalse(
      v.validate(new DepartmentController.Create("A".repeat(101))).isEmpty()
    );
    assertTrue(
      v
        .validate(new DepartmentController.Create("Research & Development"))
        .isEmpty()
    );
  }

  @Test
  void validatesLeaveAndRejectionText() {
    var date = LocalDate.of(2026, 11, 12);
    assertFalse(
      v.validate(new LeaveDto.Create(null, date, date, "short")).isEmpty()
    );
    assertFalse(v.validate(new LeaveDto.Reject("  short   ")).isEmpty());
    // Apply the same trimming rule to submitted leave reasons.
    assertFalse(
      v
        .validate(
          new LeaveDto.Create(
            com.omibits.leave.leave.LeaveRequest.LeaveType.MEDICAL,
            date,
            date,
            "  short   "
          )
        )
        .isEmpty()
    );
    // Padding must not cause a too-short message to pass validation.
    assertFalse(v.validate(new LeaveDto.Reject("          ")).isEmpty());
    // Maximum raw input length remains limited to 1000 characters.
    assertFalse(v.validate(new LeaveDto.Reject("A".repeat(1001))).isEmpty());
    assertTrue(
      v
        .validate(new LeaveDto.Reject("Not enough available coverage today."))
        .isEmpty()
    );
    assertTrue(
      v.validate(new LeaveDto.Reject("  Not enough coverage.  ")).isEmpty()
    );
  }

  @Test
  void resetRequiresStrongPassword() {
    assertFalse(
      v
        .validate(new AuthController.Reset("reset-token", "weakpassword"))
        .isEmpty()
    );
    assertTrue(
      v
        .validate(new AuthController.Reset("reset-token", "NewPassword#42"))
        .isEmpty()
    );
  }
}
