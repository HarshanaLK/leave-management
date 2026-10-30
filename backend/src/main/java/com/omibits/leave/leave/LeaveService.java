package com.omibits.leave.leave;

import com.omibits.leave.common.ApiException;
import com.omibits.leave.employee.UserAccount;
import com.omibits.leave.employee.UserRepository;
import com.omibits.leave.notification.NotificationService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Set;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveService {

  private static final Set<String> SORTABLE = Set.of(
    "id",
    "createdAt",
    "startDate",
    "endDate",
    "status",
    "leaveType",
    "employeeName",
    "employeeEmail",
    "department",
    "gender",
    "reason"
  );
  private final LeaveRepository leaves;
  private final UserRepository users;
  private final NotificationService emails;

  public LeaveService(
    LeaveRepository leaves,
    UserRepository users,
    NotificationService emails
  ) {
    this.leaves = leaves;
    this.users = users;
    this.emails = emails;
  }

  // Company policy: requests cannot start in the past (Asia/Colombo business date).
  // Accept 'today' and future dates. Historical requests remain queryable.
  public static void validateDates(LocalDate start, LocalDate end) {
    validateDates(start, end, LocalDate.now(ZoneId.of("Asia/Colombo")));
  }

  public static void validateDates(
    LocalDate start,
    LocalDate end,
    LocalDate today
  ) {
    if (start == null || end == null) throw new ApiException(
      HttpStatus.BAD_REQUEST,
      "Start date and end date are required"
    );
    if (start.isBefore(today)) throw new ApiException(
      HttpStatus.BAD_REQUEST,
      "Start date cannot be before today"
    );
    if (end.isBefore(start)) throw new ApiException(
      HttpStatus.BAD_REQUEST,
      "End date must be on or after start date"
    );
  }

  public static void requireTextLength(String text, String field) {
    if (
      text == null || text.strip().length() < 10 || text.strip().length() > 1000
    ) throw new ApiException(
      HttpStatus.BAD_REQUEST,
      field + " must contain 10–1000 non-padding characters"
    );
  }

  public static void requirePending(LeaveRequest.Status status) {
    if (status != LeaveRequest.Status.PENDING) throw new ApiException(
      HttpStatus.CONFLICT,
      "Only pending requests may be reviewed"
    );
  }

  @Transactional
  public LeaveDto.View create(LeaveDto.Create request, String email) {
    validateDates(request.startDate(), request.endDate());
    requireTextLength(request.reason(), "Reason");
    UserAccount u = current(email);
    if (u.role != UserAccount.Role.EMPLOYEE) throw new ApiException(
      HttpStatus.FORBIDDEN,
      "Only employees can request leave"
    );
    var leave = leaves.save(
      new LeaveRequest(
        u,
        request.leaveType(),
        request.startDate(),
        request.endDate(),
        request.reason().strip()
      )
    );
    emails.leaveSubmitted(
      u,
      leave.id,
      leave.leaveType.name(),
      leave.startDate,
      leave.endDate
    );
    emails.newLeaveForAdmins(
      u,
      leave.id,
      leave.leaveType.name(),
      leave.startDate,
      leave.endDate
    );
    return LeaveDto.View.from(leave);
  }

  @Transactional(readOnly = true)
  public Page<LeaveDto.View> list(
    LeaveFilter filter,
    int page,
    int size,
    String sortBy,
    Sort.Direction direction,
    String email
  ) {
    filter.validate();
    if (!SORTABLE.contains(sortBy)) throw new ApiException(
      HttpStatus.BAD_REQUEST,
      "Unsupported sort field"
    );
    var me = current(email);
    Long forced = me.isAdministrator() ? null : me.id;
    String sortPath = switch (sortBy) {
      case "employeeName" -> "employee.fullName";
      case "employeeEmail" -> "employee.email";
      case "department" -> "employee.department.name";
      case "gender" -> "employee.gender";
      default -> sortBy;
    };
    return leaves
      .findAll(
        filter.spec(forced),
        PageRequest.of(
          page,
          size,
          Sort.by(direction, sortPath).and(Sort.by("id"))
        )
      )
      .map(LeaveDto.View::from);
  }

  @Transactional(readOnly = true)
  public LeaveDto.View get(Long id, String email) {
    return LeaveDto.View.from(authorized(id, email));
  }

  @Transactional
  public LeaveDto.View approve(Long id, String email) {
    return review(id, email, true, null);
  }

  @Transactional
  public LeaveDto.View reject(Long id, String email, String reason) {
    requireTextLength(reason, "Rejection reason");
    return review(id, email, false, reason.strip());
  }

  private LeaveDto.View review(
    Long id,
    String email,
    boolean approve,
    String reason
  ) {
    UserAccount admin = current(email);
    if (!admin.isAdministrator()) throw new ApiException(
      HttpStatus.FORBIDDEN,
      "Administrator only"
    );
    var leave = leaves
      .findById(id)
      .orElseThrow(() ->
        new ApiException(HttpStatus.NOT_FOUND, "Leave not found")
      );
    requirePending(leave.status);
    leave.status = approve
      ? LeaveRequest.Status.APPROVED
      : LeaveRequest.Status.REJECTED;
    leave.rejectionReason = reason;
    leave.reviewedBy = admin;
    leave.reviewedAt = Instant.now();
    // Explicit flush makes optimistic-lock conflicts visible before sending success to the client.
    leaves.saveAndFlush(leave);
    emails.leaveDecision(
      leave.employee,
      id,
      leave.startDate,
      leave.endDate,
      approve,
      reason
    );
    return LeaveDto.View.from(leave);
  }

  public LeaveRequest authorized(Long id, String email) {
    UserAccount u = current(email);
    LeaveRequest leave = leaves
      .findById(id)
      .orElseThrow(() ->
        new ApiException(HttpStatus.NOT_FOUND, "Leave not found")
      );
    if (
      !u.isAdministrator() && !leave.employee.getId().equals(u.id)
    ) throw new ApiException(
      HttpStatus.FORBIDDEN,
      "Access denied to this leave request"
    );
    return leave;
  }

  public UserAccount current(String email) {
    return users
      .findByEmailIgnoreCase(email)
      .filter(u -> u.active)
      .orElseThrow(() ->
        new ApiException(
          HttpStatus.UNAUTHORIZED,
          "Account is disabled or missing"
        )
      );
  }

  @Transactional(readOnly = true)
  public java.util.Map<String, Long> stats(String email) {
    UserAccount user = current(email);
    boolean admin = user.isAdministrator();
    return java.util.Map.of(
      "total",
      admin ? leaves.count() : leaves.countByEmployeeId(user.id),
      "pending",
      admin
        ? leaves.countByStatus(LeaveRequest.Status.PENDING)
        : leaves.countByEmployeeIdAndStatus(
            user.id,
            LeaveRequest.Status.PENDING
          ),
      "approved",
      admin
        ? leaves.countByStatus(LeaveRequest.Status.APPROVED)
        : leaves.countByEmployeeIdAndStatus(
            user.id,
            LeaveRequest.Status.APPROVED
          ),
      "rejected",
      admin
        ? leaves.countByStatus(LeaveRequest.Status.REJECTED)
        : leaves.countByEmployeeIdAndStatus(
            user.id,
            LeaveRequest.Status.REJECTED
          ),
      "employees",
      admin ? users.countByRoleAndActiveTrue(UserAccount.Role.EMPLOYEE) : 0L
    );
  }
}
