package com.omibits.leave.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.omibits.leave.common.ApiException;
import com.omibits.leave.department.Department;
import com.omibits.leave.department.DepartmentRepository;
import com.omibits.leave.employee.EmployeeDto;
import com.omibits.leave.employee.UserAccount;
import com.omibits.leave.employee.UserRepository;
import com.omibits.leave.notification.NotificationService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminServiceTest {

  AdminService service;
  UserRepository users;
  DepartmentRepository departments;

  @BeforeEach
  void setup() {
    users = mock(UserRepository.class);
    departments = mock(DepartmentRepository.class);
    service = new AdminService(
      users,
      departments,
      mock(PasswordEncoder.class),
      mock(NotificationService.class)
    );
  }

  @Test
  void unsupportedSortFieldRejected() {
    assertThrows(ApiException.class, () ->
      service.list(
        null,
        null,
        null,
        null,
        "passwordHash",
        Sort.Direction.ASC,
        0,
        10
      )
    );
    verify(users, never()).findAll(
      any(org.springframework.data.jpa.domain.Specification.class),
      any(org.springframework.data.domain.Pageable.class)
    );
  }

  @Test
  void cannotModifySuperAdminFromAdminEndpoint() {
    var superUser = new UserAccount(
      "Super",
      "root@example.test",
      "hash",
      UserAccount.Gender.OTHER,
      UserAccount.Role.SUPER_ADMIN,
      null
    );
    superUser.id = 17L;
    when(users.findById(17L)).thenReturn(Optional.of(superUser));
    var update = new EmployeeDto.Update(
      "Changed",
      "root@example.test",
      UserAccount.Gender.OTHER,
      1L,
      true
    );
    assertThrows(ApiException.class, () ->
      service.update(17L, update, "root@example.test")
    );
  }

  @Test
  void canDeactivateNormalAdmin() {
    var admin = new UserAccount(
      "Admin",
      "admin@example.test",
      "hash",
      UserAccount.Gender.OTHER,
      UserAccount.Role.ADMIN,
      null
    );
    admin.id = 9L;
    var department = new Department("Operations");
    department.id = 1L;
    when(users.findById(9L)).thenReturn(Optional.of(admin));
    when(departments.findById(1L)).thenReturn(Optional.of(department));
    var update = new EmployeeDto.Update(
      "Admin",
      "admin@example.test",
      UserAccount.Gender.OTHER,
      1L,
      false
    );
    var actual = service.update(9L, update, "root@example.test");
    assertFalse(actual.active());
    assertEquals("Operations", actual.department());
  }
}
