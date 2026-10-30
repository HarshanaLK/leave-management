package com.omibits.leave.department;

import jakarta.persistence.*;

@Entity
@Table(name = "departments")
public class Department {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(nullable = false, unique = true, length = 100)
  public String name;

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  protected Department() {}

  public Department(String name) {
    this.name = name;
  }
}
