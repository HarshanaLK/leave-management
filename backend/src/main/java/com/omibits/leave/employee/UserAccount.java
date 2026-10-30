package com.omibits.leave.employee;

import com.omibits.leave.department.Department;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "users")
public class UserAccount {

  public enum Gender {
    MALE,
    FEMALE,
    OTHER,
    PREFER_NOT_TO_SAY,
  }

  public enum Role {
    SUPER_ADMIN,
    ADMIN,
    EMPLOYEE,
  }

  public boolean isAdministrator() {
    return role == Role.ADMIN || role == Role.SUPER_ADMIN;
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "full_name", nullable = false, length = 120)
  public String fullName;

  @Column(nullable = false, unique = true, length = 190)
  public String email;

  @Column(name = "password_hash", nullable = false)
  public String passwordHash;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 24)
  public Gender gender;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  public Role role;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "department_id")
  public Department department;

  @Column(nullable = false)
  public boolean active = true;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  public Instant updatedAt;

  // Always use accessors for related entities: direct public-field reads on Hibernate lazy proxies can return null.
  public Long getId() {
    return id;
  }

  public String getFullName() {
    return fullName;
  }

  public String getEmail() {
    return email;
  }

  public Gender getGender() {
    return gender;
  }

  public Role getRole() {
    return role;
  }

  public Department getDepartment() {
    return department;
  }

  public boolean isActive() {
    return active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  protected UserAccount() {}

  public UserAccount(
    String fullName,
    String email,
    String passwordHash,
    Gender gender,
    Role role,
    Department department
  ) {
    this.fullName = fullName;
    this.email = email;
    this.passwordHash = passwordHash;
    this.gender = gender;
    this.role = role;
    this.department = department;
  }

  @PrePersist
  public void created() {
    createdAt = Instant.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  public void updated() {
    updatedAt = Instant.now();
  }
}
