package com.omibits.leave.notification;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.omibits.leave.common.ApiException;
import com.omibits.leave.employee.UserAccount;
import com.omibits.leave.employee.UserRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.util.Optional;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mail.javamail.JavaMailSender;

class NotificationServiceTest {

  private final ApplicationEventPublisher publisher = mock(
    ApplicationEventPublisher.class
  );
  private final JavaMailSender sender = mock(JavaMailSender.class);
  private final UserRepository users = mock(UserRepository.class);

  private NotificationService service(boolean enabled) {
    return new NotificationService(
      publisher,
      sender,
      users,
      enabled,
      "verified@example.test",
      "http://localhost:5173",
      "smtp-relay.brevo.com",
      "login",
      "secret",
      true
    );
  }

  @Test
  void disabledEmailDoesNotContactSmtp() {
    service(false).deliver(EmailTemplates.test("admin@example.test"));
    verifyNoInteractions(sender);
    assertThrows(ApiException.class, () ->
      service(false).testToSuperAdmin("admin@example.test")
    );
  }

  @Test
  void enabledEmailUsesHtmlAndPlainTextMimeMessage() throws Exception {
    MimeMessage mime = new MimeMessage(Session.getInstance(new Properties()));
    when(sender.createMimeMessage()).thenReturn(mime);
    service(true).deliver(EmailTemplates.test("root@example.test"));
    verify(sender).send(mime);
    mime.saveChanges();
    assertEquals("LeaveHub email connection test", mime.getSubject());
    assertEquals("root@example.test", mime.getAllRecipients()[0].toString());
    assertTrue(mime.getContentType().contains("multipart/"));
  }

  @Test
  void testTargetsAuthenticatedSuperAdminOnly() {
    var superAdmin = new UserAccount(
      "Super",
      "root@example.test",
      "hash",
      UserAccount.Gender.OTHER,
      UserAccount.Role.SUPER_ADMIN,
      null
    );
    when(users.findByEmailIgnoreCase("root@example.test")).thenReturn(
      Optional.of(superAdmin)
    );
    service(true).testToSuperAdmin("root@example.test");
    verify(publisher).publishEvent(any(EmailEvent.class));
    var employee = new UserAccount(
      "Worker",
      "worker@example.test",
      "hash",
      UserAccount.Gender.OTHER,
      UserAccount.Role.EMPLOYEE,
      null
    );
    when(users.findByEmailIgnoreCase("worker@example.test")).thenReturn(
      Optional.of(employee)
    );
    assertThrows(ApiException.class, () ->
      service(true).testToSuperAdmin("worker@example.test")
    );
  }
}
