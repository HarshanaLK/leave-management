package com.omibits.leave.leave;

import com.omibits.leave.common.TrimmedLength;
import com.omibits.leave.employee.UserAccount;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;

public final class LeaveDto {

  private LeaveDto() {}

  public record Create(
    @NotNull(message = "Choose a leave type") LeaveRequest.LeaveType leaveType,
    @NotNull(message = "Start date is required") LocalDate startDate,
    @NotNull(message = "End date is required") LocalDate endDate,
    @NotBlank(message = "Reason is required")
    @Size(max = 1000, message = "Reason must be at most 1000 characters")
    @TrimmedLength(
      min = 10,
      max = 1000,
      message = "Reason must contain 10–1000 characters excluding surrounding spaces"
    )
    String reason
  ) {}

  public record Reject(
    @NotBlank(message = "Rejection reason is required")
    @Size(
      max = 1000,
      message = "Rejection reason must be at most 1000 characters"
    )
    @TrimmedLength(
      min = 10,
      max = 1000,
      message = "Rejection reason must contain 10–1000 characters excluding surrounding spaces"
    )
    String reason
  ) {}

  public record View(
    Long id,
    Long employeeId,
    String employeeName,
    String employeeEmail,
    UserAccount.Gender gender,
    Long departmentId,
    String department,
    LeaveRequest.LeaveType leaveType,
    LocalDate startDate,
    LocalDate endDate,
    String reason,
    LeaveRequest.Status status,
    String rejectionReason,
    String reviewer,
    Instant reviewedAt,
    Instant createdAt,
    Long version
  ) {
    public static View from(LeaveRequest l) {
      var employee = l.employee;
      var department = employee.getDepartment();
      return new View(
        l.id,
        employee.getId(),
        employee.getFullName(),
        employee.getEmail(),
        employee.getGender(),
        department == null ? null : department.getId(),
        department == null ? null : department.getName(),
        l.leaveType,
        l.startDate,
        l.endDate,
        l.reason,
        l.status,
        l.rejectionReason,
        l.reviewedBy == null ? null : l.reviewedBy.getFullName(),
        l.reviewedAt,
        l.createdAt,
        l.version
      );
    }
  }
}
