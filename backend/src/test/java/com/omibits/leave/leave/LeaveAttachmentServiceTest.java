package com.omibits.leave.leave;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.omibits.leave.common.ApiException;
import com.omibits.leave.employee.UserAccount;
import com.omibits.leave.storage.AttachmentService;
import com.omibits.leave.storage.LeaveAttachment;
import com.omibits.leave.storage.LeaveAttachmentRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LeaveAttachmentServiceTest {

  @TempDir
  Path folder;

  private LeaveAttachmentRepository repo;
  private LeaveService leaveService;
  private AttachmentService attachments;
  private LeaveRequest leave;
  private UserAccount owner;
  private final byte[] pdf = "%PDF-1.7\nSample".getBytes(
    java.nio.charset.StandardCharsets.US_ASCII
  );

  @BeforeEach
  void setup() {
    repo = mock(LeaveAttachmentRepository.class);
    leaveService = mock(LeaveService.class);
    attachments = new AttachmentService(repo, leaveService, folder.toString());
    owner = new UserAccount(
      "Employee",
      "employee@example.test",
      "hash",
      UserAccount.Gender.OTHER,
      UserAccount.Role.EMPLOYEE,
      null
    );
    owner.id = 19L;
    leave = new LeaveRequest(
      owner,
      LeaveRequest.LeaveType.MEDICAL,
      LocalDate.of(2026, 11, 10),
      LocalDate.of(2026, 11, 11),
      "Medical appointment"
    );
    leave.id = 7L;
    when(leaveService.authorized(7L, owner.email)).thenReturn(leave);
  }

  private MockMultipartFile file(String filename, byte[] bytes) {
    return new MockMultipartFile(
      "file",
      filename,
      "application/octet-stream",
      bytes
    );
  }

  @Test
  void uploadDownloadAndRemovePendingMedicalReport() throws Exception {
    when(repo.save(any(LeaveAttachment.class))).thenAnswer(invocation -> {
      LeaveAttachment saved = invocation.getArgument(0);
      saved.id = 42L;
      return saved;
    });
    var dto = attachments.upload(7L, owner.email, file("report.pdf", pdf));
    assertEquals("report.pdf", dto.originalName());
    assertEquals(42L, dto.id());
    var entityCaptor = org.mockito.ArgumentCaptor.forClass(
      LeaveAttachment.class
    );
    verify(repo).save(entityCaptor.capture());
    var entity = entityCaptor.getValue();
    assertTrue(Files.exists(folder.resolve(entity.storedName)));
    when(repo.findByIdAndLeaveId(42L, 7L)).thenReturn(Optional.of(entity));
    assertArrayEquals(pdf, attachments.download(7L, 42L, owner.email).bytes());
    attachments.delete(7L, 42L, owner.email);
    verify(repo).delete(entity);
    assertFalse(Files.exists(folder.resolve(entity.storedName)));
  }

  @Test
  void rejectSpoofedDocumentContent() {
    assertThrows(ApiException.class, () ->
      attachments.upload(
        7L,
        owner.email,
        file("report.pdf", "not actually a PDF".getBytes())
      )
    );
    verify(repo, never()).save(any());
  }

  @Test
  void rejectUnsupportedExtension() {
    assertThrows(ApiException.class, () ->
      attachments.upload(7L, owner.email, file("malware.exe", pdf))
    );
    verify(repo, never()).save(any());
  }

  @Test
  void rejectMoreThanThreeDocuments() {
    when(repo.countByLeaveId(7L)).thenReturn(3L);
    assertThrows(ApiException.class, () ->
      attachments.upload(7L, owner.email, file("report.pdf", pdf))
    );
  }

  @Test
  void rejectOverFiveMegabytes() {
    assertThrows(ApiException.class, () ->
      attachments.upload(
        7L,
        owner.email,
        file("large.pdf", new byte[5 * 1024 * 1024 + 1])
      )
    );
  }

  @Test
  void administratorCannotUploadAsEmployee() {
    var admin = new UserAccount(
      "Admin",
      "admin@example.test",
      "hash",
      UserAccount.Gender.OTHER,
      UserAccount.Role.ADMIN,
      null
    );
    when(leaveService.authorized(7L, admin.email)).thenReturn(leave);
    assertThrows(ApiException.class, () ->
      attachments.upload(7L, admin.email, file("report.pdf", pdf))
    );
  }

  @Test
  void finalizedLeaveCannotReceiveNewDocumentsOrDelete() {
    leave.status = LeaveRequest.Status.APPROVED;
    assertThrows(ApiException.class, () ->
      attachments.upload(7L, owner.email, file("report.pdf", pdf))
    );
    assertThrows(ApiException.class, () ->
      attachments.delete(7L, 42L, owner.email)
    );
  }

  @Test
  void unauthorizedUserCannotReadAttachments() {
    when(leaveService.authorized(7L, "other@example.test")).thenThrow(
      new ApiException(
        org.springframework.http.HttpStatus.FORBIDDEN,
        "Access denied"
      )
    );
    assertThrows(ApiException.class, () ->
      attachments.list(7L, "other@example.test")
    );
  }
}
