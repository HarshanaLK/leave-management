package com.omibits.leave.auth;

import com.omibits.leave.employee.UserAccount;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  public UserAccount user;

  @Column(name = "token_hash", nullable = false, unique = true, length = 64)
  public String tokenHash;

  @Column(name = "expires_at", nullable = false)
  public Instant expiresAt;

  @Column(name = "used_at")
  public Instant usedAt;

  @Column(name = "created_at", nullable = false)
  public Instant createdAt;

  protected PasswordResetToken() {}

  public PasswordResetToken(UserAccount user, String tokenHash) {
    this.user = user;
    this.tokenHash = tokenHash;
    createdAt = Instant.now();
    expiresAt = createdAt.plusSeconds(30 * 60);
  }
}
