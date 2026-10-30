package com.omibits.leave.leave;

import static org.junit.jupiter.api.Assertions.*;

import com.omibits.leave.common.ApiException;
import com.omibits.leave.storage.AttachmentService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LeaveBusinessRulesTest {

  @Test
  void rejectsInvertedDates() {
    var today = LocalDate.of(2026, 10, 29);
    assertThrows(ApiException.class, () ->
      LeaveService.validateDates(today.plusDays(4), today.plusDays(2), today)
    );
  }

  @Test
  void rejectsPastStartDateEvenWhenRangeIsOtherwiseValid() {
    var today = LocalDate.of(2026, 10, 29);
    assertThrows(ApiException.class, () ->
      LeaveService.validateDates(today.minusDays(1), today.plusDays(1), today)
    );
  }

  @Test
  void allowsTodayAndFutureDateRange() {
    var today = LocalDate.of(2026, 10, 29);
    assertDoesNotThrow(() -> LeaveService.validateDates(today, today, today));
    assertDoesNotThrow(() ->
      LeaveService.validateDates(today.plusDays(1), today.plusDays(3), today)
    );
  }

  @Test
  void rejectsMissingDates() {
    var today = LocalDate.of(2026, 10, 29);
    assertThrows(ApiException.class, () ->
      LeaveService.validateDates(null, today, today)
    );
  }

  @Test
  void allowsSameDayLeave() {
    var today = LocalDate.of(2026, 10, 29);
    assertDoesNotThrow(() -> LeaveService.validateDates(today, today, today));
  }

  @Test
  void whitespacePaddingDoesNotSatisfyReasonMinimum() {
    assertThrows(ApiException.class, () ->
      LeaveService.requireTextLength("          a         ", "Reason")
    );
  }

  @Test
  void validReasonAccepted() {
    assertDoesNotThrow(() ->
      LeaveService.requireTextLength("A medical consultation", "Reason")
    );
  }

  @Test
  void statusCannotBeReviewedTwice() {
    assertThrows(ApiException.class, () ->
      LeaveService.requirePending(LeaveRequest.Status.APPROVED)
    );
    assertThrows(ApiException.class, () ->
      LeaveService.requirePending(LeaveRequest.Status.REJECTED)
    );
  }

  @Test
  void pendingIsReviewable() {
    assertDoesNotThrow(() ->
      LeaveService.requirePending(LeaveRequest.Status.PENDING)
    );
  }

  @Test
  void detectsPdfMagicBytes() {
    assertTrue(
      AttachmentService.validSignature("%PDF-1.7".getBytes(), "application/pdf")
    );
    assertFalse(
      AttachmentService.validSignature("not-pdf".getBytes(), "application/pdf")
    );
  }
}
