package com.omibits.leave.notification;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EmailTemplatesTest {

  @Test
  void resetEmailContainsOneTimeLinkInBothVersions() {
    String url = "http://localhost:5173/reset-password?token=unique-token";
    EmailEvent email = EmailTemplates.resetPassword(
      "employee@example.test",
      "Employee",
      url
    );
    assertTrue(email.textBody().contains(url));
    assertTrue(email.htmlBody().contains(url));
    assertTrue(email.textBody().contains("30 minutes"));
    assertFalse(email.htmlBody().contains("<script"));
  }

  @Test
  void untrustedUserInputIsEscapedInHtml() {
    EmailEvent email = EmailTemplates.welcome(
      "name@example.test",
      "<script>alert(1)</script>",
      "employee",
      "http://localhost:5173"
    );
    assertTrue(email.htmlBody().contains("&lt;script&gt;"));
    assertFalse(email.htmlBody().contains("<script>"));
    assertTrue(email.textBody().contains("<script>")); // text/plain does not interpret HTML
  }

  @Test
  void rejectionReasonIsEscapedAndIncludedInBothFormats() {
    EmailEvent email = EmailTemplates.leaveReview(
      "test@example.test",
      "Person",
      12L,
      LocalDate.of(2026, 11, 4),
      LocalDate.of(2026, 11, 6),
      false,
      "Missing <report>"
    );
    assertTrue(email.htmlBody().contains("Missing &lt;report&gt;"));
    assertTrue(email.textBody().contains("Missing <report>"));
    assertTrue(email.subject().contains("rejected"));
  }

  @Test
  void emailNeverIncludesAnInitialPassword() {
    EmailEvent email = EmailTemplates.welcome(
      "worker@example.test",
      "Worker",
      "employee",
      "http://localhost:5173"
    );
    assertTrue(email.textBody().contains("privately"));
    assertFalse(email.textBody().contains("Your password is"));
  }

  @Test
  void administratorNotificationContainsReviewLink() {
    EmailEvent email = EmailTemplates.adminNewLeave(
      "admin@example.test",
      "Worker",
      "worker@example.test",
      42L,
      "MEDICAL",
      LocalDate.of(2026, 12, 1),
      LocalDate.of(2026, 12, 3),
      "http://localhost:5173"
    );
    assertTrue(email.htmlBody().contains("/leaves"));
    assertTrue(email.subject().contains("42"));
  }
}
