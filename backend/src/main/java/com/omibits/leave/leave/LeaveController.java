package com.omibits.leave.leave;

import com.omibits.leave.employee.UserAccount;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/leaves")
public class LeaveController {

  private final LeaveService service;

  public LeaveController(LeaveService service) {
    this.service = service;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasRole('EMPLOYEE')")
  public LeaveDto.View create(
    @Valid @RequestBody LeaveDto.Create body,
    Authentication a
  ) {
    return service.create(body, a.getName());
  }

  @GetMapping
  public ResponseEntity<Page<LeaveDto.View>> list(
    @RequestParam(required = false) @Size(
      max = 120,
      message = "Search must be 120 characters or fewer"
    ) String search,
    @RequestParam(required = false) LeaveRequest.Status status,
    @RequestParam(required = false) LeaveRequest.LeaveType leaveType,
    @RequestParam(required = false) @Positive(
      message = "Invalid department ID"
    ) Long departmentId,
    @RequestParam(required = false) UserAccount.Gender gender,
    @RequestParam(required = false) @Positive(
      message = "Invalid employee ID"
    ) Long employeeId,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE
    ) LocalDate startFrom,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE
    ) LocalDate startTo,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE
    ) LocalDate endFrom,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE
    ) LocalDate endTo,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE
    ) LocalDate submittedFrom,
    @RequestParam(required = false) @DateTimeFormat(
      iso = DateTimeFormat.ISO.DATE
    ) LocalDate submittedTo,
    @RequestParam(defaultValue = "createdAt") String sortBy,
    @RequestParam(defaultValue = "DESC") Sort.Direction direction,
    @RequestParam(defaultValue = "0") @Min(0) int page,
    @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
    Authentication a
  ) {
    var results = service.list(
      new LeaveFilter(
        search,
        status,
        leaveType,
        departmentId,
        gender,
        employeeId,
        startFrom,
        startTo,
        endFrom,
        endTo,
        submittedFrom,
        submittedTo
      ),
      page,
      size,
      sortBy,
      direction,
      a.getName()
    );
    return ResponseEntity.ok()
      .cacheControl(CacheControl.noStore())
      .header("X-Query-Engine", "SPRING_DATA_JPA")
      .header("X-Total-Records", String.valueOf(results.getTotalElements()))
      .body(results);
  }

  @GetMapping("/stats")
  public Map<String, Long> stats(Authentication a) {
    return service.stats(a.getName());
  }

  @GetMapping("/{id}")
  public LeaveDto.View get(@PathVariable Long id, Authentication a) {
    return service.get(id, a.getName());
  }

  @PatchMapping("/{id}/approve")
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
  public LeaveDto.View approve(@PathVariable Long id, Authentication a) {
    return service.approve(id, a.getName());
  }

  @PatchMapping("/{id}/reject")
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
  public LeaveDto.View reject(
    @PathVariable Long id,
    @Valid @RequestBody LeaveDto.Reject body,
    Authentication a
  ) {
    return service.reject(id, a.getName(), body.reason());
  }
}
