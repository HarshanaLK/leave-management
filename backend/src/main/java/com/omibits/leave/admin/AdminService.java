package com.omibits.leave.admin;

import com.omibits.leave.common.ApiException;
import com.omibits.leave.department.Department;
import com.omibits.leave.department.DepartmentRepository;
import com.omibits.leave.employee.EmployeeDto;
import com.omibits.leave.employee.UserAccount;
import com.omibits.leave.employee.UserRepository;
import com.omibits.leave.notification.NotificationService;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

  private static final Map<String, String> SORT_FIELDS = Map.of(
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
  private final UserRepository users;
  private final DepartmentRepository departments;
  private final PasswordEncoder encoder;
  private final NotificationService email;

  public AdminService(
    UserRepository users,
    DepartmentRepository departments,
    PasswordEncoder encoder,
    NotificationService email
  ) {
    this.users = users;
    this.departments = departments;
    this.encoder = encoder;
    this.email = email;
  }

  @Transactional(readOnly = true)
  public Page<EmployeeDto.View> list(
    String search,
    UserAccount.Gender gender,
    Long departmentId,
    Boolean active,
    String sortBy,
    Sort.Direction direction,
    int page,
    int size
  ) {
    String sortField = SORT_FIELDS.get(sortBy);
    if (sortField == null) throw new ApiException(
      HttpStatus.BAD_REQUEST,
      "Unsupported administrator sort field"
    );
    Specification<UserAccount> spec = (root, query, cb) -> {
      var conditions = new ArrayList<Predicate>();
      // Super-admin accounts are intentionally excluded from editable admin directory.
      conditions.add(cb.equal(root.get("role"), UserAccount.Role.ADMIN));
      if (search != null && !search.isBlank()) {
        String q = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        conditions.add(
          cb.or(
            cb.like(cb.lower(root.get("fullName")), q),
            cb.like(cb.lower(root.get("email")), q)
          )
        );
      }
      if (gender != null) conditions.add(cb.equal(root.get("gender"), gender));
      if (departmentId != null) conditions.add(
        cb.equal(root.get("department").get("id"), departmentId)
      );
      if (active != null) conditions.add(cb.equal(root.get("active"), active));
      return cb.and(conditions.toArray(Predicate[]::new));
    };
    return users
      .findAll(
        spec,
        PageRequest.of(
          page,
          size,
          Sort.by(direction, sortField).and(Sort.by("id"))
        )
      )
      .map(EmployeeDto.View::from);
  }

  @Transactional
  public EmployeeDto.View create(EmployeeDto.Create data) {
    String address = data.email().trim().toLowerCase(Locale.ROOT);
    if (users.existsByEmailIgnoreCase(address)) throw new ApiException(
      HttpStatus.CONFLICT,
      "Email already registered"
    );
    Department department = departments
      .findById(data.departmentId())
      .orElseThrow(() ->
        new ApiException(HttpStatus.BAD_REQUEST, "Invalid department")
      );
    var account = users.save(
      new UserAccount(
        data.fullName().trim(),
        address,
        encoder.encode(data.password()),
        data.gender(),
        UserAccount.Role.ADMIN,
        department
      )
    );
    email.welcome(account);
    return EmployeeDto.View.from(account);
  }

  @Transactional
  public EmployeeDto.View update(
    Long id,
    EmployeeDto.Update data,
    String currentEmail
  ) {
    UserAccount account = users
      .findById(id)
      .orElseThrow(() ->
        new ApiException(HttpStatus.NOT_FOUND, "Administrator not found")
      );
    if (account.role != UserAccount.Role.ADMIN) throw new ApiException(
      HttpStatus.FORBIDDEN,
      "Super administrators and employees cannot be edited here"
    );
    if (
      account.email.equalsIgnoreCase(currentEmail) && !data.active()
    ) throw new ApiException(
      HttpStatus.CONFLICT,
      "You cannot deactivate your own account"
    );
    String address = data.email().trim().toLowerCase(Locale.ROOT);
    if (
      !account.email.equals(address) && users.existsByEmailIgnoreCase(address)
    ) throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
    Department department = departments
      .findById(data.departmentId())
      .orElseThrow(() ->
        new ApiException(HttpStatus.BAD_REQUEST, "Invalid department")
      );
    account.fullName = data.fullName().trim();
    account.email = address;
    account.gender = data.gender();
    account.department = department;
    account.active = data.active();
    return EmployeeDto.View.from(account);
  }
}
