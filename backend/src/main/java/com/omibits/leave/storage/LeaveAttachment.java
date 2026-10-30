package com.omibits.leave.storage;

import com.omibits.leave.leave.LeaveRequest;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "leave_attachments")
public class LeaveAttachment {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "leave_id", nullable = false)
  public LeaveRequest leave;

  @Column(name = "original_name", nullable = false, length = 255)
  public String originalName;

  @Column(name = "stored_name", nullable = false, unique = true, length = 100)
  public String storedName;

  @Column(name = "content_type", nullable = false, length = 70)
  public String contentType;

  @Column(name = "size_bytes", nullable = false)
  public long sizeBytes;

  @Column(name = "uploaded_at", nullable = false)
  public Instant uploadedAt;

  protected LeaveAttachment() {}

  public LeaveAttachment(
    LeaveRequest leave,
    String originalName,
    String storedName,
    String contentType,
    long sizeBytes
  ) {
    this.leave = leave;
    this.originalName = originalName;
    this.storedName = storedName;
    this.contentType = contentType;
    this.sizeBytes = sizeBytes;
    uploadedAt = Instant.now();
  }
}
