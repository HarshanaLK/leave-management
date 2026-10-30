package com.omibits.leave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class LeaveManagementApplication {

  public static void main(String[] args) {
    SpringApplication.run(LeaveManagementApplication.class, args);
  }
}
