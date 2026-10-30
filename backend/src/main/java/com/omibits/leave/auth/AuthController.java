package com.omibits.leave.auth;

import com.omibits.leave.common.InputRules;
import com.omibits.leave.employee.EmployeeDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService service;

  public AuthController(AuthService service) {
    this.service = service;
  }

  public record Login(
    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 190)
    String email,
    @NotBlank(message = "Password is required")
    @Size(max = 128, message = "Password is too long")
    String password
  ) {}

  public record Forgot(
    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 190)
    String email
  ) {}

  public record Reset(
    @NotBlank(message = "Reset token is required")
    @Size(max = 256)
    String token,
    @NotBlank(message = "New password is required")
    @Pattern(
      regexp = InputRules.STRONG_PASSWORD,
      message = "Use 8–72 characters with uppercase, lowercase, number and symbol; no spaces"
    )
    String newPassword
  ) {}

  @GetMapping("/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken());
  }

  @PostMapping("/login")
  public EmployeeDto.View login(
    @Valid @RequestBody Login data,
    HttpServletRequest request,
    HttpServletResponse response
  ) {
    return service.login(data.email(), data.password(), request, response);
  }

  @PostMapping("/logout")
  public Map<String, String> logout(
    HttpServletRequest request,
    HttpServletResponse response
  ) {
    service.logout(request, response);
    return Map.of("message", "Signed out");
  }

  @GetMapping("/me")
  public EmployeeDto.View me(Authentication auth) {
    return service.current(auth.getName());
  }

  @PostMapping("/forgot-password")
  public Map<String, String> forgot(@Valid @RequestBody Forgot data) {
    service.forgot(data.email());
    return Map.of(
      "message",
      "If the account exists, a reset email will be sent."
    );
  }

  @PostMapping("/reset-password")
  public Map<String, String> reset(
    @Valid @RequestBody Reset data,
    HttpServletRequest request,
    HttpServletResponse response
  ) {
    service.reset(data.token(), data.newPassword());
    // The browser may already have a valid session from before the reset.
    // Invalidate this session so the login screen actually checks the new password.
    new SecurityContextLogoutHandler().logout(
      request,
      response,
      SecurityContextHolder.getContext().getAuthentication()
    );
    return Map.of(
      "message",
      "Password updated. Please sign in with your new password."
    );
  }
}
