package com.omibits.leave.leave;

import com.omibits.leave.employee.UserAccount;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public record LeaveFilter(
  String search,
  LeaveRequest.Status status,
  LeaveRequest.LeaveType leaveType,
  Long departmentId,
  UserAccount.Gender gender,
  Long employeeId,
  LocalDate startFrom,
  LocalDate startTo,
  LocalDate endFrom,
  LocalDate endTo,
  LocalDate submittedFrom,
  LocalDate submittedTo
) {
  public Specification<LeaveRequest> spec(Long restrictedEmployeeId) {
    return (root, query, cb) -> {
      var p = new ArrayList<Predicate>();
      if (restrictedEmployeeId != null) p.add(
        cb.equal(root.get("employee").get("id"), restrictedEmployeeId)
      );
      else if (employeeId != null) p.add(
        cb.equal(root.get("employee").get("id"), employeeId)
      );
      if (search != null && !search.isBlank()) {
        String s = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        p.add(
          cb.or(
            cb.like(cb.lower(root.get("employee").get("fullName")), s),
            cb.like(cb.lower(root.get("employee").get("email")), s),
            cb.like(cb.lower(root.get("reason")), s),
            // A request ID like 42 (or #42) can be searched directly too.
            search.trim().replaceFirst("^#", "").matches("[0-9]{1,15}")
              ? cb.equal(
                  root.get("id"),
                  Long.parseLong(search.trim().replaceFirst("^#", ""))
                )
              : cb.disjunction()
          )
        );
      }
      if (status != null) p.add(cb.equal(root.get("status"), status));
      if (leaveType != null) p.add(cb.equal(root.get("leaveType"), leaveType));
      if (departmentId != null) p.add(
        cb.equal(root.get("employee").get("department").get("id"), departmentId)
      );
      if (gender != null) p.add(
        cb.equal(root.get("employee").get("gender"), gender)
      );
      if (startFrom != null) p.add(
        cb.greaterThanOrEqualTo(root.get("startDate"), startFrom)
      );
      if (startTo != null) p.add(
        cb.lessThanOrEqualTo(root.get("startDate"), startTo)
      );
      if (endFrom != null) p.add(
        cb.greaterThanOrEqualTo(root.get("endDate"), endFrom)
      );
      if (endTo != null) p.add(
        cb.lessThanOrEqualTo(root.get("endDate"), endTo)
      );
      if (submittedFrom != null) p.add(
        cb.greaterThanOrEqualTo(
          root.get("createdAt"),
          submittedFrom.atStartOfDay(java.time.ZoneOffset.UTC).toInstant()
        )
      );
      if (submittedTo != null) p.add(
        cb.lessThan(
          root.get("createdAt"),
          submittedTo
            .plusDays(1)
            .atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant()
        )
      );
      return cb.and(p.toArray(Predicate[]::new));
    };
  }

  public void validate() {
    if (
      search != null && search.length() > 120
    ) throw new IllegalArgumentException(
      "Search must be at most 120 characters"
    );
    if (
      employeeId != null && employeeId <= 0
    ) throw new IllegalArgumentException("Employee ID must be positive");
    if (
      departmentId != null && departmentId <= 0
    ) throw new IllegalArgumentException("Department ID must be positive");
    if (
      startFrom != null && startTo != null && startTo.isBefore(startFrom)
    ) throw new IllegalArgumentException("Invalid start date filter range");
    if (
      endFrom != null && endTo != null && endTo.isBefore(endFrom)
    ) throw new IllegalArgumentException("Invalid end date filter range");
    if (
      submittedFrom != null &&
      submittedTo != null &&
      submittedTo.isBefore(submittedFrom)
    ) throw new IllegalArgumentException(
      "Invalid submission date filter range"
    );
  }
}
