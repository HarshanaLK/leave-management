package com.omibits.leave.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.omibits.leave.employee.UserAccount;
import com.omibits.leave.employee.UserRepository;
import com.omibits.leave.notification.NotificationService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

class PasswordResetServiceTest {

  @Test
  void resetUpdatesDatabaseHashAndConsumesToken() {
    var users = mock(UserRepository.class);
    var tokens = mock(PasswordResetRepository.class);
    var email = mock(NotificationService.class);
    var encoder = new BCryptPasswordEncoder(4);
    var service = new AuthService(
      users,
      tokens,
      encoder,
      mock(AuthenticationManager.class),
      mock(HttpSessionSecurityContextRepository.class),
      email,
      "http://localhost:5173"
    );

    var account = new UserAccount(
      "Test Employee",
      "person@example.test",
      encoder.encode("OldPassword@123"),
      UserAccount.Gender.OTHER,
      UserAccount.Role.EMPLOYEE,
      null
    );
    account.id = 5L;
    var token = new PasswordResetToken(account, "sha256-digest");
    when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));
    when(users.findById(5L)).thenReturn(Optional.of(account));
    when(
      users.updatePasswordHash(eq(5L), anyString(), any(Instant.class))
    ).thenAnswer(invocation -> {
      account.passwordHash = invocation.getArgument(1);
      return 1;
    });

    service.reset("one-time-token", "NewPassword@123");

    assertNotNull(token.usedAt, "Reset token must be consumed");
    assertTrue(encoder.matches("NewPassword@123", account.passwordHash));
    assertFalse(encoder.matches("OldPassword@123", account.passwordHash));
    verify(users).updatePasswordHash(eq(5L), anyString(), any(Instant.class));
    verify(tokens).saveAndFlush(token);
    verify(email).passwordChanged(account);
    assertThrows(
      RuntimeException.class,
      () -> service.reset("one-time-token", "AnotherPassword@123"),
      "A consumed token cannot be reused"
    );
  }

  @Test
  void resetFailsIfDatabaseUpdatedNoRows() {
    var users = mock(UserRepository.class);
    var tokens = mock(PasswordResetRepository.class);
    var encoder = new BCryptPasswordEncoder(4);
    var service = new AuthService(
      users,
      tokens,
      encoder,
      mock(AuthenticationManager.class),
      mock(HttpSessionSecurityContextRepository.class),
      mock(NotificationService.class),
      "http://localhost:5173"
    );

    var account = new UserAccount(
      "Test Employee",
      "person@example.test",
      encoder.encode("OldPassword@123"),
      UserAccount.Gender.OTHER,
      UserAccount.Role.EMPLOYEE,
      null
    );
    account.id = 5L;
    when(tokens.findByTokenHash(anyString())).thenReturn(
      Optional.of(new PasswordResetToken(account, "digest"))
    );
    when(users.findById(5L)).thenReturn(Optional.of(account));
    when(
      users.updatePasswordHash(eq(5L), anyString(), any(Instant.class))
    ).thenReturn(0);

    assertThrows(IllegalStateException.class, () ->
      service.reset("token", "NewPassword@123")
    );
  }
}
