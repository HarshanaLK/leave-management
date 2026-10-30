package com.omibits.leave.employee;

import static org.junit.jupiter.api.Assertions.*;

import com.omibits.leave.department.Department;
import com.omibits.leave.department.DepartmentRepository;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;

/** Tests actual JPA database queries, not filtering a fetched Java list. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(
  properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
  }
)
class EmployeeRepositoryQueryTest {

  @Autowired
  UserRepository users;

  @Autowired
  DepartmentRepository departments;

  private Department engineering;
  private Department hr;

  @BeforeEach
  void seed() {
    engineering = departments.save(new Department("Engineering"));
    hr = departments.save(new Department("Human Resources"));
    users.save(
      new UserAccount(
        "Maya Perera",
        "maya@example.test",
        "hash",
        UserAccount.Gender.FEMALE,
        UserAccount.Role.EMPLOYEE,
        engineering
      )
    );
    users.save(
      new UserAccount(
        "Amal Silva",
        "amal@example.test",
        "hash",
        UserAccount.Gender.MALE,
        UserAccount.Role.EMPLOYEE,
        hr
      )
    );
    users.save(
      new UserAccount(
        "Zara Fernando",
        "zara@example.test",
        "hash",
        UserAccount.Gender.FEMALE,
        UserAccount.Role.EMPLOYEE,
        engineering
      )
    );
    users.save(
      new UserAccount(
        "Super User",
        "super@example.test",
        "hash",
        UserAccount.Gender.OTHER,
        UserAccount.Role.SUPER_ADMIN,
        null
      )
    );
    users.flush();
  }

  private Specification<UserAccount> filter(
    String search,
    UserAccount.Gender gender,
    Long deptId,
    Boolean active
  ) {
    return (root, query, cb) -> {
      var terms =
        new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
      terms.add(cb.equal(root.get("role"), UserAccount.Role.EMPLOYEE));
      if (search != null && !search.isBlank()) {
        String q = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        terms.add(
          cb.or(
            cb.like(cb.lower(root.get("fullName")), q),
            cb.like(cb.lower(root.get("email")), q)
          )
        );
      }
      if (gender != null) terms.add(cb.equal(root.get("gender"), gender));
      if (deptId != null) terms.add(
        cb.equal(root.get("department").get("id"), deptId)
      );
      if (active != null) terms.add(cb.equal(root.get("active"), active));
      return cb.and(
        terms.toArray(jakarta.persistence.criteria.Predicate[]::new)
      );
    };
  }

  @Test
  void searchingAndGenderAndDepartmentAreAppliedInTheDatabase() {
    var page = users.findAll(
      filter("a", UserAccount.Gender.FEMALE, engineering.getId(), true),
      PageRequest.of(0, 10, Sort.by("fullName"))
    );
    assertEquals(2, page.getTotalElements());
    assertEquals("Maya Perera", page.getContent().get(0).getFullName());
    assertEquals("Zara Fernando", page.getContent().get(1).getFullName());
    assertEquals(
      0,
      users
        .findAll(
          filter("does-not-exist", null, null, null),
          PageRequest.of(0, 10)
        )
        .getTotalElements()
    );
  }

  @Test
  void databaseOrderingAndPaginationCoverAllMatchingRows() {
    var first = users.findAll(
      filter(null, null, null, null),
      PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "fullName"))
    );
    var second = users.findAll(
      filter(null, null, null, null),
      PageRequest.of(1, 1, Sort.by(Sort.Direction.ASC, "fullName"))
    );
    assertEquals(3, first.getTotalElements());
    assertEquals(3, second.getTotalElements());
    assertEquals("Amal Silva", first.getContent().get(0).getFullName());
    assertEquals("Maya Perera", second.getContent().get(0).getFullName());
  }
}
