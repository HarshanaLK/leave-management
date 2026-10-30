package com.omibits.leave.auth;

import static org.junit.jupiter.api.Assertions.*;

import com.omibits.leave.employee.UserAccount;
import com.omibits.leave.employee.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@DataJpaTest(
  properties = {
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create-drop",
  }
)
class PasswordHashPersistenceTest {

  @Autowired
  UserRepository users;

  @Autowired
  TestEntityManager em;

  @Test
  void passwordUpdateIsVisibleAfterClearingJPAContext() {
    var encoder = new BCryptPasswordEncoder(4);
    var user = new UserAccount(
      "Test User",
      "test@example.test",
      encoder.encode("OldPassword@123"),
      UserAccount.Gender.PREFER_NOT_TO_SAY,
      UserAccount.Role.EMPLOYEE,
      null
    );
    Long id = em.persistAndGetId(user, Long.class);
    em.flush();
    em.clear();

    String newHash = encoder.encode("NewPassword@123");
    assertEquals(1, users.updatePasswordHash(id, newHash, Instant.now()));
    em.clear();

    var saved = users.findById(id).orElseThrow();
    assertTrue(encoder.matches("NewPassword@123", saved.passwordHash));
    assertFalse(encoder.matches("OldPassword@123", saved.passwordHash));
  }
}
