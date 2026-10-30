package com.omibits.leave.leave;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.omibits.leave.common.ApiException;
import com.omibits.leave.employee.UserAccount;
import com.omibits.leave.employee.UserRepository;
import com.omibits.leave.notification.NotificationService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeaveServiceTest {

  private LeaveRepository leaves;
  private UserRepository users;
  private NotificationService notifications;
  private LeaveService service;
  private UserAccount admin;
  private LeaveRequest leave;

  @BeforeEach
  void setup() {
    leaves = mock(LeaveRepository.class);
    users = mock(UserRepository.class);
    notifications = mock(NotificationService.class);
    service = new LeaveService(leaves, users, notifications);
    admin = new UserAccount(
      "Admin",
      "admin@test.invalid",
      "hash",
      UserAccount.Gender.OTHER,
      UserAccount.Role.ADMIN,
      null
    );
    admin.id = 1L;
    var employee = new UserAccount(
      "Test Person",
      "employee@test.invalid",
      "hash",
      UserAccount.Gender.FEMALE,
      UserAccount.Role.EMPLOYEE,
      null
    );
    employee.id = 2L;
    leave = new LeaveRequest(
      employee,
      LeaveRequest.LeaveType.MEDICAL,
      LocalDate.of(2026, 11, 5),
      LocalDate.of(2026, 11, 6),
      "Medical appointment"
    );
    leave.id = 10L;
    when(users.findByEmailIgnoreCase(admin.email)).thenReturn(
      Optional.of(admin)
    );
    when(leaves.findById(10L)).thenReturn(Optional.of(leave));
    when(leaves.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  void adminCanApprovePendingLeave() {
    var result = service.approve(10L, admin.email);
    assertEquals(LeaveRequest.Status.APPROVED, result.status());
    assertEquals("Admin", result.reviewer());
    verify(notifications).leaveDecision(
      eq(leave.employee),
      eq(10L),
      eq(leave.startDate),
      eq(leave.endDate),
      eq(true),
      isNull()
    );
  }

  @Test
  void approvedLeaveCannotBeRejectedLater() {
    service.approve(10L, admin.email);
    assertThrows(ApiException.class, () ->
      service.reject(10L, admin.email, "A valid reason")
    );
    verify(leaves, times(1)).saveAndFlush(any());
  }

  @Test
  void rejectionRequiresReason() {
    assertThrows(ApiException.class, () ->
      service.reject(10L, admin.email, " ")
    );
    verify(leaves, never()).saveAndFlush(any());
  }

  @Test
  void employeeCannotApprove() {
    when(users.findByEmailIgnoreCase(leave.employee.email)).thenReturn(
      Optional.of(leave.employee)
    );
    assertThrows(ApiException.class, () ->
      service.approve(10L, leave.employee.email)
    );
    verify(leaves, never()).saveAndFlush(any());
  }

  @Test
  void superAdminCanApprovePendingLeave() {
    var superUser = new UserAccount(
      "Super",
      "super@test.invalid",
      "hash",
      UserAccount.Gender.OTHER,
      UserAccount.Role.SUPER_ADMIN,
      null
    );
    superUser.id = 3L;
    when(users.findByEmailIgnoreCase(superUser.email)).thenReturn(
      Optional.of(superUser)
    );
    assertEquals(
      LeaveRequest.Status.APPROVED,
      service.approve(10L, superUser.email).status()
    );
  }

  @Test
  void unauthorizedSortFieldRejected() {
    assertThrows(ApiException.class, () ->
      service.list(
        new LeaveFilter(
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null,
          null
        ),
        0,
        10,
        "employee.passwordHash",
        org.springframework.data.domain.Sort.Direction.ASC,
        admin.email
      )
    );
  }

  @SuppressWarnings("null")
  @Test
  void actualPageQueryPassesSpecificationAndPageableToRepository() {
    when(
      leaves.findAll(
        any(org.springframework.data.jpa.domain.Specification.class),
        any(org.springframework.data.domain.Pageable.class)
      )
    ).thenReturn(org.springframework.data.domain.Page.empty());
    var filter = new LeaveFilter(
      "Doctor",
      LeaveRequest.Status.PENDING,
      LeaveRequest.LeaveType.MEDICAL,
      null,
      UserAccount.Gender.FEMALE,
      null,
      null,
      null,
      null,
      null,
      null,
      null
    );
    service.list(
      filter,
      2,
      5,
      "startDate",
      org.springframework.data.domain.Sort.Direction.ASC,
      admin.email
    );
    var page = org.mockito.ArgumentCaptor.forClass(
      org.springframework.data.domain.Pageable.class
    );
    verify(leaves).findAll(
      any(org.springframework.data.jpa.domain.Specification.class),
      page.capture()
    );
    assertEquals(2, page.getValue().getPageNumber());
    assertEquals(5, page.getValue().getPageSize());
    assertEquals(
      org.springframework.data.domain.Sort.Direction.ASC,
      page.getValue().getSort().getOrderFor("startDate").getDirection()
    );
  }

  @Test
  void filterDateRangeValidation() {
    var filter = new LeaveFilter(
      null,
      null,
      null,
      null,
      null,
      null,
      LocalDate.of(2026, 11, 6),
      LocalDate.of(2026, 11, 5),
      null,
      null,
      null,
      null
    );
    assertThrows(IllegalArgumentException.class, filter::validate);
  }
}
