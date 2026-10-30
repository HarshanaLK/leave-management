package com.omibits.leave.admin;

import com.omibits.leave.employee.EmployeeDto;
import com.omibits.leave.employee.UserAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/admins")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

  private final AdminService service;

  public AdminController(AdminService service) {
    this.service = service;
  }

  @GetMapping
  public ResponseEntity<Page<EmployeeDto.View>> list(
    @RequestParam(required = false) @Size(
      max = 120,
      message = "Search must be 120 characters or fewer"
    ) String search,
    @RequestParam(required = false) UserAccount.Gender gender,
    @RequestParam(required = false) @Positive(
      message = "Invalid department ID"
    ) Long departmentId,
    @RequestParam(required = false) Boolean active,
    @RequestParam(defaultValue = "createdAt") String sortBy,
    @RequestParam(defaultValue = "DESC") Sort.Direction direction,
    @RequestParam(defaultValue = "0") @Min(0) int page,
    @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
  ) {
    var results = service.list(
      search,
      gender,
      departmentId,
      active,
      sortBy,
      direction,
      page,
      size
    );
    return ResponseEntity.ok()
      .cacheControl(CacheControl.noStore())
      .header("X-Query-Engine", "SPRING_DATA_JPA")
      .header("X-Total-Records", String.valueOf(results.getTotalElements()))
      .body(results);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EmployeeDto.View create(@Valid @RequestBody EmployeeDto.Create input) {
    return service.create(input);
  }

  @PatchMapping("/{id}")
  public EmployeeDto.View update(
    @PathVariable Long id,
    @Valid @RequestBody EmployeeDto.Update input,
    Authentication auth
  ) {
    return service.update(id, input, auth.getName());
  }
}
