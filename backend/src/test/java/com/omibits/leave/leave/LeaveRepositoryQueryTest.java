package com.omibits.leave.leave;

import static org.junit.jupiter.api.Assertions.*;

import com.omibits.leave.department.Department;
import com.omibits.leave.department.DepartmentRepository;
import com.omibits.leave.employee.UserAccount;
import com.omibits.leave.employee.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(
  properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
  }
)
class LeaveRepositoryQueryTest {

  @Autowired
  LeaveRepository leaves;

  @Autowired
  UserRepository users;

  @Autowired
  DepartmentRepository departments;

  private UserAccount employee1;
  private UserAccount employee2;

  @BeforeEach
  void seed() {
    var eng = departments.save(new Department("Engineering"));
    var hr = departments.save(new Department("HR"));
    employee1 = users.save(
      new UserAccount(
        "Amy Kumar",
        "amy@example.test",
        "hash",
        UserAccount.Gender.FEMALE,
        UserAccount.Role.EMPLOYEE,
        eng
      )
    );
    employee2 = users.save(
      new UserAccount(
        "Ben Lee",
        "ben@example.test",
        "hash",
        UserAccount.Gender.MALE,
        UserAccount.Role.EMPLOYEE,
        hr
      )
    );
    var a = new LeaveRequest(
      employee1,
      LeaveRequest.LeaveType.MEDICAL,
      LocalDate.of(2026, 11, 1),
      LocalDate.of(2026, 11, 3),
      "Doctor appointment"
    );
    var b = new LeaveRequest(
      employee2,
      LeaveRequest.LeaveType.ANNUAL,
      LocalDate.of(2026, 12, 1),
      LocalDate.of(2026, 12, 2),
      "Holiday"
    );
    var c = new LeaveRequest(
      employee1,
      LeaveRequest.LeaveType.ANNUAL,
      LocalDate.of(2026, 10, 5),
      LocalDate.of(2026, 10, 8),
      "Family holiday"
    );
    b.status = LeaveRequest.Status.APPROVED;
    leaves.save(a);
    leaves.save(b);
    leaves.save(c);
    leaves.flush();
  }

  private LeaveFilter filter(
    String search,
    LeaveRequest.Status status,
    LeaveRequest.LeaveType type,
    Long department,
    UserAccount.Gender gender,
    Long employee,
    LocalDate startFrom,
    LocalDate startTo
  ) {
    return new LeaveFilter(
      search,
      status,
      type,
      department,
      gender,
      employee,
      startFrom,
      startTo,
      null,
      null,
      null,
      null
    );
  }

  @Test
  void filtersSearchByReasonWithStatusAndDatesInsideSql() {
    var page = leaves.findAll(
      filter(
        "Holiday",
        LeaveRequest.Status.APPROVED,
        LeaveRequest.LeaveType.ANNUAL,
        employee2.getDepartment().getId(),
        UserAccount.Gender.MALE,
        null,
        LocalDate.of(2026, 11, 1),
        LocalDate.of(2026, 12, 31)
      ).spec(null),
      PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "startDate"))
    );
    assertEquals(1, page.getTotalElements());
    assertEquals(employee2.getId(), page.getContent().get(0).employee.getId());
  }

  @Test
  void employeesCanOnlyQueryTheirOwnLeaveEvenWithSomeoneElsesId() {
    var f = filter(null, null, null, null, null, employee2.getId(), null, null);
    var page = leaves.findAll(f.spec(employee1.getId()), PageRequest.of(0, 10));
    assertEquals(2, page.getTotalElements());
    assertTrue(
      page
        .getContent()
        .stream()
        .allMatch(l -> l.employee.getId().equals(employee1.getId()))
    );
  }

  @Test
  void sortingRunsBeforeLimitAndOffset() {
    var first = leaves.findAll(
      filter(null, null, null, null, null, null, null, null).spec(null),
      PageRequest.of(0, 1, Sort.by("startDate"))
    );
    var second = leaves.findAll(
      filter(null, null, null, null, null, null, null, null).spec(null),
      PageRequest.of(1, 1, Sort.by("startDate"))
    );
    assertEquals(3, first.getTotalElements());
    assertEquals(
      LocalDate.of(2026, 10, 5),
      first.getContent().get(0).startDate
    );
    assertEquals(
      LocalDate.of(2026, 11, 1),
      second.getContent().get(0).startDate
    );
  }
}
