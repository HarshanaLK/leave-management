package com.omibits.leave.auth;

import com.omibits.leave.common.ApiException;
import com.omibits.leave.employee.EmployeeDto;
import com.omibits.leave.employee.UserAccount;
import com.omibits.leave.employee.UserRepository;
import com.omibits.leave.notification.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository users;
  private final PasswordResetRepository tokens;
  private final PasswordEncoder encoder;
  private final AuthenticationManager manager;
  private final HttpSessionSecurityContextRepository contexts;
  private final NotificationService emails;
  private final String frontend;
  private final SecureRandom random = new SecureRandom();

  public AuthService(
    UserRepository users,
    PasswordResetRepository tokens,
    PasswordEncoder encoder,
    AuthenticationManager manager,
    HttpSessionSecurityContextRepository contexts,
    NotificationService emails,
    @Value("${app.frontend-url}") String frontend
  ) {
    this.users = users;
    this.tokens = tokens;
    this.encoder = encoder;
    this.manager = manager;
    this.contexts = contexts;
    this.emails = emails;
    this.frontend = frontend;
  }

  @Transactional(readOnly = true)
  public EmployeeDto.View login(
    String email,
    String password,
    HttpServletRequest request,
    HttpServletResponse response
  ) {
    Authentication auth = manager.authenticate(
      new UsernamePasswordAuthenticationToken(
        email.trim().toLowerCase(Locale.ROOT),
        password
      )
    );
    if (request.getSession(false) != null) request.changeSessionId();
    var context = new SecurityContextImpl(auth);
    SecurityContextHolder.setContext(context);
    contexts.saveContext(context, request, response);
    return current(auth.getName());
  }

  public void logout(HttpServletRequest request, HttpServletResponse response) {
    new SecurityContextLogoutHandler().logout(
      request,
      response,
      SecurityContextHolder.getContext().getAuthentication()
    );
  }

  @Transactional(readOnly = true)
  public EmployeeDto.View current(String email) {
    return EmployeeDto.View.from(
      users
        .findByEmailIgnoreCase(email)
        .orElseThrow(() ->
          new ApiException(HttpStatus.UNAUTHORIZED, "Account not found")
        )
    );
  }

  @Transactional
  public void forgot(String email) {
    users
      .findByEmailIgnoreCase(email.trim())
      .filter(u -> u.active)
      .ifPresent(u -> {
        byte[] randomBytes = new byte[32];
        random.nextBytes(randomBytes);
        String raw = Base64.getUrlEncoder()
          .withoutPadding()
          .encodeToString(randomBytes);
        tokens.save(new PasswordResetToken(u, sha256(raw)));
        emails.passwordReset(u, frontend + "/reset-password?token=" + raw);
      });
  }

  @Transactional
  public void reset(String rawToken, String newPassword) {
    PasswordResetToken t = tokens
      .findByTokenHash(sha256(rawToken))
      .orElseThrow(() -> badToken());
    if (
      t.usedAt != null || !t.expiresAt.isAfter(Instant.now())
    ) throw badToken();
    // A reset must persist a new hash in the same database used by login.
    // A JPQL update checks affected rows and avoids relying on field dirty checking.
    UserAccount account = users
      .findById(t.user.getId())
      .orElseThrow(() -> badToken());
    if (!account.isActive()) throw badToken();
    String newHash = encoder.encode(newPassword);
    if (!encoder.matches(newPassword, newHash)) {
      throw new IllegalStateException("Password encoder verification failed");
    }
    t.usedAt = Instant.now();
    tokens.saveAndFlush(t); // flush before @Modifying clears the persistence context
    int changed = users.updatePasswordHash(
      account.getId(),
      newHash,
      Instant.now()
    );
    if (changed != 1) {
      throw new IllegalStateException(
        "Password reset did not update exactly one account"
      );
    }
    // updatePasswordHash clears the persistence context; reread to confirm DB result.
    UserAccount updated = users
      .findById(account.getId())
      .orElseThrow(() ->
        new IllegalStateException("Account missing after password reset")
      );
    if (!encoder.matches(newPassword, updated.passwordHash)) {
      throw new IllegalStateException("Password reset was not persisted");
    }
    emails.passwordChanged(updated);
  }

  private ApiException badToken() {
    return new ApiException(
      HttpStatus.BAD_REQUEST,
      "Invalid or expired reset token"
    );
  }

  private String sha256(String value) {
    try {
      return HexFormat.of().formatHex(
        MessageDigest.getInstance("SHA-256").digest(
          value.getBytes(StandardCharsets.UTF_8)
        )
      );
    } catch (Exception e) {
      throw new IllegalStateException("SHA-256 unavailable", e);
    }
  }
}
