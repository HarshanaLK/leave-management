package com.omibits.leave.storage;

import com.omibits.leave.common.ApiException;
import com.omibits.leave.leave.LeaveRequest;
import com.omibits.leave.leave.LeaveService;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AttachmentService {

  public record AttachmentDto(
    Long id,
    String originalName,
    String contentType,
    long sizeBytes,
    String downloadUrl
  ) {
    public static AttachmentDto from(LeaveAttachment a) {
      return new AttachmentDto(
        a.id,
        a.originalName,
        a.contentType,
        a.sizeBytes,
        "/api/leaves/" + a.leave.getId() + "/attachments/" + a.id + "/download"
      );
    }
  }

  public record Download(byte[] bytes, String contentType, String filename) {}

  private final LeaveAttachmentRepository repo;
  private final LeaveService service;
  private final Path directory;

  public AttachmentService(
    LeaveAttachmentRepository repo,
    LeaveService service,
    @Value("${app.upload-directory}") String directory
  ) {
    this.repo = repo;
    this.service = service;
    this.directory = Paths.get(directory).toAbsolutePath().normalize();
  }

  @Transactional(readOnly = true)
  public List<AttachmentDto> list(Long leaveId, String email) {
    service.authorized(leaveId, email);
    return repo
      .findByLeaveIdOrderByUploadedAtDesc(leaveId)
      .stream()
      .map(AttachmentDto::from)
      .toList();
  }

  @Transactional
  public AttachmentDto upload(Long leaveId, String email, MultipartFile file) {
    LeaveRequest leave = service.authorized(leaveId, email);
    if (
      !leave.employee.getEmail().equalsIgnoreCase(email)
    ) throw new ApiException(
      HttpStatus.FORBIDDEN,
      "Only request owner may upload documents"
    );
    LeaveService.requirePending(leave.status);
    if (repo.countByLeaveId(leaveId) >= 3) throw new ApiException(
      HttpStatus.BAD_REQUEST,
      "Maximum 3 files per leave request"
    );
    if (
      file.isEmpty() || file.getSize() > 5 * 1024 * 1024
    ) throw new ApiException(
      HttpStatus.BAD_REQUEST,
      "Document must be between 1 byte and 5 MB"
    );
    String original = Optional.ofNullable(file.getOriginalFilename()).orElse(
      "document"
    );
    original = original.replace('\\', '/');
    original = original.substring(original.lastIndexOf('/') + 1);
    original = original.replaceAll("[\\p{Cntrl}]", "").trim();
    if (original.isBlank() || original.length() > 255) throw new ApiException(
      HttpStatus.BAD_REQUEST,
      "Invalid filename"
    );
    String ext = original.contains(".")
      ? original
          .substring(original.lastIndexOf('.') + 1)
          .toLowerCase(Locale.ROOT)
      : "";
    String contentType = switch (ext) {
      case "pdf" -> "application/pdf";
      case "jpg", "jpeg" -> "image/jpeg";
      case "png" -> "image/png";
      default -> throw new ApiException(
        HttpStatus.BAD_REQUEST,
        "Allowed formats: PDF, JPG, PNG"
      );
    };
    try {
      byte[] head;
      try (var in = file.getInputStream()) {
        head = in.readNBytes(8);
      }
      if (!validSignature(head, contentType)) throw new ApiException(
        HttpStatus.BAD_REQUEST,
        "File content does not match its type"
      );
      Files.createDirectories(directory);
      String stored = UUID.randomUUID().toString() + "." + ext;
      Path destination = directory.resolve(stored).normalize();
      if (!destination.startsWith(directory)) throw new ApiException(
        HttpStatus.BAD_REQUEST,
        "Unsafe file path"
      );
      try (var input = file.getInputStream()) {
        Files.copy(input, destination);
      }
      try {
        return AttachmentDto.from(
          repo.save(
            new LeaveAttachment(
              leave,
              original,
              stored,
              contentType,
              file.getSize()
            )
          )
        );
      } catch (RuntimeException exception) {
        Files.deleteIfExists(destination);
        throw exception;
      }
    } catch (IOException e) {
      throw new ApiException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Could not store document"
      );
    }
  }

  @Transactional(readOnly = true)
  public Download download(Long leaveId, Long attachmentId, String email) {
    service.authorized(leaveId, email);
    LeaveAttachment a = repo
      .findByIdAndLeaveId(attachmentId, leaveId)
      .orElseThrow(() ->
        new ApiException(HttpStatus.NOT_FOUND, "Document not found")
      );
    try {
      return new Download(
        Files.readAllBytes(directory.resolve(a.storedName).normalize()),
        a.contentType,
        a.originalName
      );
    } catch (IOException e) {
      throw new ApiException(
        HttpStatus.NOT_FOUND,
        "Document unavailable on disk"
      );
    }
  }

  @Transactional
  public void delete(Long leaveId, Long attachmentId, String email) {
    LeaveRequest leave = service.authorized(leaveId, email);
    if (
      !leave.employee.getEmail().equalsIgnoreCase(email)
    ) throw new ApiException(
      HttpStatus.FORBIDDEN,
      "Only request owner can delete documents"
    );
    LeaveService.requirePending(leave.status);
    LeaveAttachment a = repo
      .findByIdAndLeaveId(attachmentId, leaveId)
      .orElseThrow(() ->
        new ApiException(HttpStatus.NOT_FOUND, "Document not found")
      );
    repo.delete(a);
    // This local-disk MVP deletes after the DB operation. Production should use object storage + background cleanup.
    try {
      Files.deleteIfExists(directory.resolve(a.storedName).normalize());
    } catch (IOException e) {
      throw new ApiException(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Could not remove document"
      );
    }
  }

  public static boolean validSignature(byte[] b, String mime) {
    if (mime.equals("application/pdf")) return (
      b.length >= 5 &&
      b[0] == '%' &&
      b[1] == 'P' &&
      b[2] == 'D' &&
      b[3] == 'F' &&
      b[4] == '-'
    );
    if (mime.equals("image/jpeg")) return (
      b.length >= 3 &&
      (b[0] & 255) == 255 &&
      (b[1] & 255) == 216 &&
      (b[2] & 255) == 255
    );
    if (mime.equals("image/png")) return (
      b.length >= 8 &&
      (b[0] & 255) == 137 &&
      b[1] == 80 &&
      b[2] == 78 &&
      b[3] == 71 &&
      b[4] == 13 &&
      b[5] == 10 &&
      b[6] == 26 &&
      b[7] == 10
    );
    return false;
  }
}
