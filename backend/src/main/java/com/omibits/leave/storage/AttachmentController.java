package com.omibits.leave.storage;

import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

@RestController
@RequestMapping("/api/leaves/{leaveId}/attachments")
public class AttachmentController {

  private final AttachmentService service;

  public AttachmentController(AttachmentService service) {
    this.service = service;
  }

  @GetMapping
  public List<AttachmentService.AttachmentDto> list(
    @PathVariable Long leaveId,
    Authentication a
  ) {
    return service.list(leaveId, a.getName());
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @ResponseStatus(HttpStatus.CREATED)
  public AttachmentService.AttachmentDto upload(
    @PathVariable Long leaveId,
    @RequestParam("file") MultipartFile file,
    Authentication a
  ) {
    return service.upload(leaveId, a.getName(), file);
  }

  @GetMapping("/{id}/download")
  public ResponseEntity<byte[]> download(
    @PathVariable Long leaveId,
    @PathVariable Long id,
    Authentication a
  ) {
    var result = service.download(leaveId, id, a.getName());
    // RFC 5987 encoding protects against unsafe response-header filenames.
    String encoded = UriUtils.encode(result.filename(), StandardCharsets.UTF_8);
    return ResponseEntity.ok()
      .contentType(MediaType.parseMediaType(result.contentType()))
      .header(
        HttpHeaders.CONTENT_DISPOSITION,
        "attachment; filename*=UTF-8''" + encoded
      )
      .header("X-Content-Type-Options", "nosniff")
      .cacheControl(CacheControl.noStore())
      .body(result.bytes());
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(
    @PathVariable Long leaveId,
    @PathVariable Long id,
    Authentication a
  ) {
    service.delete(leaveId, id, a.getName());
  }
}
