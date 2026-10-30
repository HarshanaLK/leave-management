package com.omibits.leave.employee;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/employees")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class EmployeeController {

  private final EmployeeService service;

  public EmployeeController(EmployeeService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public EmployeeDto.View create(@Valid @RequestBody EmployeeDto.Create body) {
    return service.create(body);
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
    @RequestParam(defaultValue = "0") @Min(0) int page,
    @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
    @RequestParam(defaultValue = "createdAt") String sortBy,
    @RequestParam(
      defaultValue = "DESC"
    ) org.springframework.data.domain.Sort.Direction direction
  ) {
    var results = service.list(
      search,
      gender,
      departmentId,
      active,
      page,
      size,
      sortBy,
      direction
    );
    return ResponseEntity.ok()
      .cacheControl(CacheControl.noStore())
      .header("X-Query-Engine", "SPRING_DATA_JPA")
      .header("X-Total-Records", String.valueOf(results.getTotalElements()))
      .body(results);
  }

  @PatchMapping("/{id}")
  public EmployeeDto.View update(
    @PathVariable Long id,
    @Valid @RequestBody EmployeeDto.Update body
  ) {
    return service.update(id, body);
  }
}
