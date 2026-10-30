package com.omibits.leave.notification;

/** Immutable email payload. Only publish events after sensitive values have been validated. */
public record EmailEvent(
  String to,
  String subject,
  String textBody,
  String htmlBody
) {}
