package com.omibits.leave.leave;

import com.omibits.leave.employee.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

  public enum LeaveType {
    ANNUAL,
    CASUAL,
    MEDICAL,
  }

  public enum Status {
    PENDING,
    APPROVED,
    REJECTED,
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "employee_id", nullable = false)
  public UserAccount employee;

  @Enumerated(EnumType.STRING)
  @Column(name = "leave_type", nullable = false, length = 20)
  public LeaveType leaveType;

  @Column(name = "start_date", nullable = false)
  public LocalDate startDate;

  @Column(name = "end_date", nullable = false)
  public LocalDate endDate;

  @Column(nullable = false, length = 1000)
  public String reason;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  public Status status = Status.PENDING;

  @Column(name = "rejection_reason", length = 1000)
  public String rejectionReason;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "reviewed_by")
  public UserAccount reviewedBy;

  @Column(name = "reviewed_at")
  public Instant reviewedAt;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;

  @Version
  @Column(nullable = false)
  public Long version = 0L;

  public Long getId() {
    return id;
  }

  protected LeaveRequest() {}

  public LeaveRequest(
    UserAccount employee,
    LeaveType leaveType,
    LocalDate startDate,
    LocalDate endDate,
    String reason
  ) {
    this.employee = employee;
    this.leaveType = leaveType;
    this.startDate = startDate;
    this.endDate = endDate;
    this.reason = reason;
  }

  @PrePersist
  void created() {
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  void updated() {
    updatedAt = Instant.now();
  }
}
