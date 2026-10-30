package com.omibits.leave.leave;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public interface LeaveRepository
  extends
    JpaRepository<LeaveRequest, Long>,
    JpaSpecificationExecutor<LeaveRequest>
{
  @Override
  @EntityGraph(
    attributePaths = { "employee", "employee.department", "reviewedBy" }
  )
  @NonNull
  Page<LeaveRequest> findAll(
    @Nullable Specification<LeaveRequest> spec,
    @NonNull Pageable pageable
  );

  @Override
  @EntityGraph(
    attributePaths = { "employee", "employee.department", "reviewedBy" }
  )
  @NonNull
  java.util.Optional<LeaveRequest> findById(@NonNull Long id);

  long countByStatus(LeaveRequest.Status status);
  long countByEmployeeIdAndStatus(Long employeeId, LeaveRequest.Status status);
  long countByEmployeeId(Long employeeId);
}
