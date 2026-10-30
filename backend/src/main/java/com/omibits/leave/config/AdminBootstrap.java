package com.omibits.leave.config;

import com.omibits.leave.common.ApiException;
import com.omibits.leave.employee.UserAccount;
import com.omibits.leave.employee.UserRepository;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class AdminBootstrap {

  private static final Logger log = LoggerFactory.getLogger(
    AdminBootstrap.class
  );

  @Bean
  CommandLineRunner administratorBootstrap(
    UserRepository users,
    PasswordEncoder encoder,
    TransactionTemplate tx,
    @Value("${app.admin.email:}") String adminEmail,
    @Value("${app.admin.password:}") String adminPassword,
    @Value("${app.super-admin.email:}") String superEmail,
    @Value("${app.super-admin.password:}") String superPassword
  ) {
    return args -> {
      // Setting SUPER_ADMIN_EMAIL explicitly upgrades a matching existing ADMIN; never an EMPLOYEE.
      bootstrap(
        users,
        encoder,
        tx,
        superEmail,
        superPassword,
        UserAccount.Role.SUPER_ADMIN
      );
      bootstrap(
        users,
        encoder,
        tx,
        adminEmail,
        adminPassword,
        UserAccount.Role.ADMIN
      );
      if (superEmail.isBlank()) log.warn(
        "SUPER_ADMIN_EMAIL not configured; set it in .env to enable administrator management"
      );
    };
  }

  private void bootstrap(
    UserRepository users,
    PasswordEncoder encoder,
    TransactionTemplate tx,
    String rawEmail,
    String password,
    UserAccount.Role targetRole
  ) {
    if (rawEmail.isBlank() || password.isBlank()) return;
    if (password.length() < 12) throw new IllegalArgumentException(
      targetRole + "_PASSWORD must be 12+ characters"
    );
    String email = rawEmail.trim().toLowerCase(Locale.ROOT);
    tx.executeWithoutResult(status -> {
      var existing = users.findByEmailIgnoreCase(email);
      if (existing.isEmpty()) {
        users.save(
          new UserAccount(
            targetRole == UserAccount.Role.SUPER_ADMIN
              ? "Super Administrator"
              : "System Administrator",
            email,
            encoder.encode(password),
            UserAccount.Gender.PREFER_NOT_TO_SAY,
            targetRole,
            null
          )
        );
        log.info("Bootstrap {} created: {}", targetRole, email);
      } else if (
        targetRole == UserAccount.Role.SUPER_ADMIN &&
        existing.get().role == UserAccount.Role.ADMIN
      ) {
        existing.get().role = UserAccount.Role.SUPER_ADMIN;
        existing.get().active = true;
        log.info(
          "Existing administrator {} promoted to SUPER_ADMIN (password preserved)",
          email
        );
      } else if (
        targetRole == UserAccount.Role.SUPER_ADMIN &&
        existing.get().role == UserAccount.Role.EMPLOYEE
      ) {
        throw new ApiException(
          HttpStatus.CONFLICT,
          "SUPER_ADMIN_EMAIL is already assigned to an employee"
        );
      }
    });
  }
}
