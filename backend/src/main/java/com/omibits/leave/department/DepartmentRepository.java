package com.omibits.leave.department;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {
  List<Department> findAllByOrderByNameAsc();
  boolean existsByNameIgnoreCase(String name);
}
