package com.omibits.leave.storage;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveAttachmentRepository
  extends JpaRepository<LeaveAttachment, Long>
{
  List<LeaveAttachment> findByLeaveIdOrderByUploadedAtDesc(Long leaveId);
  long countByLeaveId(Long leaveId);
  Optional<LeaveAttachment> findByIdAndLeaveId(Long id, Long leaveId);
}
