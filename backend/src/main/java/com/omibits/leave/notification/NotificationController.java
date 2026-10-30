package com.omibits.leave.notification;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class NotificationController {

  private final NotificationService service;

  public NotificationController(NotificationService service) {
    this.service = service;
  }

  @GetMapping("/status")
  public Map<String, Object> status() {
    return service.publicStatus();
  }

  @PostMapping("/test-email")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Map<String, String> testEmail(Authentication auth) {
    service.testToSuperAdmin(auth.getName());
    return Map.of(
      "message",
      "Test email queued. Check your inbox and Spring Boot logs for the delivery result."
    );
  }
}
