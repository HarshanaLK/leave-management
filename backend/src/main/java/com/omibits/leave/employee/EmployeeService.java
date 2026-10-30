package com.omibits.leave.employee;

import com.omibits.leave.common.ApiException;
import com.omibits.leave.department.Department;
import com.omibits.leave.department.DepartmentRepository;
import com.omibits.leave.notification.NotificationService;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Locale;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeService {

  private final UserRepository users;
  private final DepartmentRepository departments;
  private final PasswordEncoder encoder;
  private final NotificationService notifications;

  public EmployeeService(
    UserRepository users,
    DepartmentRepository departments,
    PasswordEncoder encoder,
    NotificationService notifications
  ) {
    this.users = users;
    this.departments = departments;
    this.encoder = encoder;
    this.notifications = notifications;
  }

  @Transactional
  public EmployeeDto.View create(EmployeeDto.Create input) {
    String email = input.email().trim().toLowerCase(Locale.ROOT);
    if (users.existsByEmailIgnoreCase(email)) throw new ApiException(
      HttpStatus.CONFLICT,
      "Email already registered"
    );
    var department = department(input.departmentId());
    var account = users.save(
      new UserAccount(
        input.fullName().trim(),
        email,
        encoder.encode(input.password()),
        input.gender(),
        UserAccount.Role.EMPLOYEE,
        department
      )
    );
    notifications.welcome(account);
    return EmployeeDto.View.from(account);
  }

  private static final java.util.Map<String, String> SORT_FIELDS =
    java.util.Map.of(
      "id",
      "id",
      "fullName",
      "fullName",
      "email",
      "email",
      "gender",
      "gender",
      "department",
      "department.name",
      "createdAt",
      "createdAt",
      "active",
      "active"
    );

  @Transactional(readOnly = true)
  public Page<EmployeeDto.View> list(
    String search,
    UserAccount.Gender gender,
    Long departmentId,
    Boolean active,
    int page,
    int size,
    String sortBy,
    Sort.Direction direction
  ) {
    String property = SORT_FIELDS.get(sortBy);
    if (property == null) throw new ApiException(
      HttpStatus.BAD_REQUEST,
      "Unsupported employee sort field"
    );
    Specification<UserAccount> spec = (root, query, cb) -> {
      var terms = new ArrayList<Predicate>();
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
      if (departmentId != null) terms.add(
        cb.equal(root.get("department").get("id"), departmentId)
      );
      if (active != null) terms.add(cb.equal(root.get("active"), active));
      return cb.and(terms.toArray(Predicate[]::new));
    };
    return users
      .findAll(
        spec,
        PageRequest.of(
          page,
          size,
          Sort.by(direction, property).and(Sort.by("id"))
        )
      )
      .map(EmployeeDto.View::from);
  }

  @Transactional
  public EmployeeDto.View update(Long id, EmployeeDto.Update input) {
    UserAccount u = users
      .findById(id)
      .orElseThrow(() ->
        new ApiException(HttpStatus.NOT_FOUND, "Employee not found")
      );
    if (u.role != UserAccount.Role.EMPLOYEE) throw new ApiException(
      HttpStatus.FORBIDDEN,
      "Cannot edit administrator using employee endpoint"
    );
    String email = input.email().trim().toLowerCase(Locale.ROOT);
    if (
      !u.email.equals(email) && users.existsByEmailIgnoreCase(email)
    ) throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
    u.fullName = input.fullName().trim();
    u.email = email;
    u.gender = input.gender();
    u.department = department(input.departmentId());
    u.active = input.active();
    return EmployeeDto.View.from(u);
  }

  private Department department(Long id) {
    return departments
      .findById(id)
      .orElseThrow(() ->
        new ApiException(HttpStatus.BAD_REQUEST, "Invalid department")
      );
  }
}
