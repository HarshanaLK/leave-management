package com.omibits.leave.department;

import com.omibits.leave.common.ApiException;
import com.omibits.leave.common.InputRules;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

  private final DepartmentRepository repo;

  public DepartmentController(DepartmentRepository repo) {
    this.repo = repo;
  }

  public record DepartmentDto(Long id, String name) {}

  public record Create(
    @NotBlank(message = "Department is required")
    @Size(min = 2, max = 100, message = "Department must be 2–100 characters")
    @Pattern(
      regexp = InputRules.DEPARTMENT_NAME,
      message = "Use letters, digits, spaces, and common department punctuation"
    )
    String name
  ) {}

  @GetMapping
  public List<DepartmentDto> all() {
    return repo
      .findAllByOrderByNameAsc()
      .stream()
      .map(d -> new DepartmentDto(d.id, d.name))
      .toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
  public DepartmentDto create(@Valid @RequestBody Create input) {
    String name = input.name().trim();
    if (repo.existsByNameIgnoreCase(name)) throw new ApiException(
      HttpStatus.CONFLICT,
      "Department already exists"
    );
    var d = repo.save(new Department(name));
    return new DepartmentDto(d.id, d.name);
  }
}
