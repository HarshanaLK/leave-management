package com.omibits.leave.notification;

import com.omibits.leave.common.ApiException;
import com.omibits.leave.employee.UserAccount;
import com.omibits.leave.employee.UserRepository;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class NotificationService {

  private static final Logger log = LoggerFactory.getLogger(
    NotificationService.class
  );
  private final ApplicationEventPublisher publisher;
  private final JavaMailSender sender;
  private final UserRepository users;
  private final boolean enabled;
  private final String from;
  private final String frontend;
  private final String smtpHost;
  private final String smtpUsername;
  private final String smtpPassword;
  private final boolean smtpAuth;

  public NotificationService(
    ApplicationEventPublisher publisher,
    JavaMailSender sender,
    UserRepository users,
    @Value("${app.mail.enabled:false}") boolean enabled,
    @Value("${app.mail.from}") String from,
    @Value("${app.frontend-url}") String frontend,
    @Value("${spring.mail.host:localhost}") String smtpHost,
    @Value("${spring.mail.username:}") String smtpUsername,
    @Value("${spring.mail.password:}") String smtpPassword,
    @Value("${spring.mail.properties.mail.smtp.auth:false}") boolean smtpAuth
  ) {
    this.publisher = publisher;
    this.sender = sender;
    this.users = users;
    this.enabled = enabled;
    this.from = from;
    this.frontend = frontend;
    this.smtpHost = smtpHost;
    this.smtpUsername = smtpUsername;
    this.smtpPassword = smtpPassword;
    this.smtpAuth = smtpAuth;
  }

  public void send(EmailEvent email) {
    // Even when disabled, an event is harmless and the listener logs non-sensitive metadata.
    publisher.publishEvent(email);
  }

  public void welcome(UserAccount account) {
    send(
      EmailTemplates.welcome(
        account.email,
        account.fullName,
        account.role.name().toLowerCase().replace('_', ' '),
        frontend
      )
    );
  }

  public void passwordReset(UserAccount account, String resetUrl) {
    send(
      EmailTemplates.resetPassword(account.email, account.fullName, resetUrl)
    );
  }

  public void passwordChanged(UserAccount account) {
    send(EmailTemplates.passwordChanged(account.email, account.fullName));
  }

  public void leaveSubmitted(
    UserAccount account,
    Long id,
    String leaveType,
    LocalDate start,
    LocalDate end
  ) {
    send(
      EmailTemplates.leaveSubmitted(
        account.email,
        account.fullName,
        id,
        leaveType,
        start,
        end
      )
    );
  }

  public void newLeaveForAdmins(
    UserAccount employee,
    Long id,
    String leaveType,
    LocalDate start,
    LocalDate end
  ) {
    users
      .findByRoleInAndActiveTrue(
        List.of(UserAccount.Role.ADMIN, UserAccount.Role.SUPER_ADMIN)
      )
      .forEach(admin ->
        send(
          EmailTemplates.adminNewLeave(
            admin.email,
            employee.fullName,
            employee.email,
            id,
            leaveType,
            start,
            end,
            frontend
          )
        )
      );
  }

  public void leaveDecision(
    UserAccount account,
    Long id,
    LocalDate start,
    LocalDate end,
    boolean approved,
    String reason
  ) {
    send(
      EmailTemplates.leaveReview(
        account.email,
        account.fullName,
        id,
        start,
        end,
        approved,
        reason
      )
    );
  }

  /** Accessible only to SUPER_ADMIN via NotificationController. Never returns credentials. */
  public Map<String, Object> publicStatus() {
    boolean configured =
      !from.isBlank() &&
      from.contains("@") &&
      !smtpHost.isBlank() &&
      (!smtpAuth || (!smtpUsername.isBlank() && !smtpPassword.isBlank()));
    return Map.of(
      "enabled",
      enabled,
      "configured",
      configured,
      "sender",
      from,
      "provider",
      smtpHost.equalsIgnoreCase("smtp-relay.brevo.com")
        ? "Brevo SMTP"
        : "Custom SMTP",
      "smtpHost",
      smtpHost
    );
  }

  public void testToSuperAdmin(String authenticatedEmail) {
    if (!enabled) throw new ApiException(
      HttpStatus.SERVICE_UNAVAILABLE,
      "Email is disabled. Configure MAIL_ENABLED=true and SMTP credentials in the root .env file."
    );
    if (
      !Boolean.TRUE.equals(publicStatus().get("configured"))
    ) throw new ApiException(
      HttpStatus.SERVICE_UNAVAILABLE,
      "SMTP configuration is incomplete."
    );
    var admin = users
      .findByEmailIgnoreCase(authenticatedEmail)
      .filter(u -> u.active && u.role == UserAccount.Role.SUPER_ADMIN)
      .orElseThrow(() ->
        new ApiException(HttpStatus.FORBIDDEN, "Super administrator only")
      );
    send(EmailTemplates.test(admin.email));
  }

  @Async
  @TransactionalEventListener(
    phase = TransactionPhase.AFTER_COMMIT,
    fallbackExecution = true
  )
  public void deliver(EmailEvent event) {
    if (!enabled) {
      log.info(
        "Email disabled (not delivered): recipient={} subject={}",
        event.to(),
        event.subject()
      );
      return;
    }
    try {
      MimeMessage message = sender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
      helper.setFrom(from, "LeaveHub");
      helper.setTo(event.to());
      helper.setSubject(event.subject());
      helper.setText(event.textBody(), event.htmlBody());
      sender.send(message);
      log.info(
        "Email accepted by SMTP server: recipient={} subject={}",
        event.to(),
        event.subject()
      );
    } catch (Exception e) {
      // Never log message contents: reset URLs contain single-use tokens.
      log.warn(
        "Email delivery failed for recipient={} subject={}: {}",
        event.to(),
        event.subject(),
        e.toString()
      );
    }
  }
}
