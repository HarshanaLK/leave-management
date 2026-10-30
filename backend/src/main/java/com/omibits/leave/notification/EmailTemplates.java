package com.omibits.leave.notification;

import java.time.LocalDate;

/** Safe transactional emails; all dynamic content is HTML-escaped before rendering. */
public final class EmailTemplates {

  private EmailTemplates() {}

  public static EmailEvent welcome(
    String to,
    String name,
    String role,
    String frontendUrl
  ) {
    String title = "Your LeaveHub account is ready";
    String text =
      "Hello " +
      name +
      ",\n\nYour " +
      role +
      " account has been created. " +
      "Sign in at " +
      frontendUrl +
      "/login using the initial password shared privately by your administrator. " +
      "For security, change your password if necessary and never share it by email.";
    String html = card(
      title,
      "Hello " + escape(name) + ",",
      "Your " +
        escape(role) +
        " account has been created. Your administrator will provide your initial password privately.",
      "Open LeaveHub",
      frontendUrl + "/login",
      "Never share your password by email."
    );
    return new EmailEvent(to, title, text, html);
  }

  public static EmailEvent resetPassword(
    String to,
    String name,
    String resetUrl
  ) {
    String title = "Reset your LeaveHub password";
    String text =
      "Hello " +
      name +
      ",\n\nUse this one-time link to reset your password (expires in 30 minutes):\n" +
      resetUrl +
      "\n\nIf you did not request a reset, you can ignore this message.";
    String html = card(
      title,
      "Hello " + escape(name) + ",",
      "Someone requested a password reset for your account. This link expires in 30 minutes and works only once.",
      "Reset password",
      resetUrl,
      "If this wasn't you, ignore this message. Your password has not changed."
    );
    return new EmailEvent(to, title, text, html);
  }

  public static EmailEvent passwordChanged(String to, String name) {
    String title = "Your LeaveHub password was changed";
    String text =
      "Hello " +
      name +
      ",\n\nYour account password was changed. If this wasn't you, contact your administrator immediately.";
    return new EmailEvent(
      to,
      title,
      text,
      card(
        title,
        "Hello " + escape(name) + ",",
        "Your password was updated successfully.",
        null,
        null,
        "If you did not make this change, contact your administrator immediately."
      )
    );
  }

  public static EmailEvent leaveSubmitted(
    String to,
    String name,
    Long id,
    String leaveType,
    LocalDate start,
    LocalDate end
  ) {
    String title = "Leave request #" + id + " received";
    String detail =
      "Your " +
      leaveType +
      " leave request (" +
      start +
      " to " +
      end +
      ") is pending review.";
    return new EmailEvent(
      to,
      title,
      "Hello " + name + ",\n\n" + detail,
      card(
        title,
        "Hello " + escape(name) + ",",
        escape(detail),
        null,
        null,
        "We will email you when an administrator approves or rejects the request."
      )
    );
  }

  public static EmailEvent leaveReview(
    String to,
    String name,
    Long id,
    LocalDate start,
    LocalDate end,
    boolean approved,
    String reason
  ) {
    String title =
      "Leave request #" + id + (approved ? " approved" : " rejected");
    String detail =
      "Your leave request for " +
      start +
      " to " +
      end +
      " has been " +
      (approved ? "approved." : "rejected.");
    String extra = reason == null ? "" : "\nReason: " + reason;
    String htmlDetail =
      escape(detail) +
      (reason == null
        ? ""
        : "<br><br><strong>Reason:</strong> " + escape(reason));
    return new EmailEvent(
      to,
      title,
      "Hello " + name + ",\n\n" + detail + extra,
      card(
        title,
        "Hello " + escape(name) + ",",
        htmlDetail,
        null,
        null,
        "Sign in to LeaveHub to view your leave history."
      )
    );
  }

  public static EmailEvent adminNewLeave(
    String to,
    String employee,
    String email,
    Long id,
    String leaveType,
    LocalDate start,
    LocalDate end,
    String frontendUrl
  ) {
    String title = "New leave request #" + id + " needs review";
    String detail =
      "Employee: " +
      employee +
      " (" +
      email +
      ")\nType: " +
      leaveType +
      "\nDates: " +
      start +
      " to " +
      end;
    String html = card(
      title,
      "A new leave request was submitted.",
      "<strong>Employee:</strong> " +
        escape(employee) +
        " (" +
        escape(email) +
        ")<br>" +
        "<strong>Type:</strong> " +
        escape(leaveType) +
        "<br>" +
        "<strong>Dates:</strong> " +
        start +
        " to " +
        end,
      "Review leave requests",
      frontendUrl + "/leaves",
      "Only authorized administrators can approve or reject leave."
    );
    return new EmailEvent(
      to,
      title,
      detail + "\nReview in " + frontendUrl + "/leaves",
      html
    );
  }

  public static EmailEvent test(String to) {
    String title = "LeaveHub email connection test";
    String detail =
      "Your SMTP integration delivered this test email. Employee and administrator notifications use the same connection.";
    return new EmailEvent(
      to,
      title,
      detail,
      card(
        title,
        "SMTP configuration test",
        escape(detail),
        null,
        null,
        "If you received this email, the SMTP connection was successful at the time it was sent."
      )
    );
  }

  private static String card(
    String title,
    String greeting,
    String body,
    String action,
    String url,
    String footer
  ) {
    String button =
      action == null
        ? ""
        : "<p style='margin:24px 0'><a href='" +
          escape(url) +
          "' style='background:#586bd6;color:#ffffff;" +
          "padding:12px 20px;border-radius:8px;text-decoration:none;font-weight:bold;display:inline-block'>" +
          escape(action) +
          "</a></p>";
    return (
      "<!doctype html><html lang='en'><head><meta charset='utf-8'>" +
      "<meta name='viewport' content='width=device-width,initial-scale=1'></head>" +
      "<body style='margin:0;background:#f4f7fc;font-family:Arial,sans-serif;color:#263451'>" +
      "<div style='max-width:580px;margin:30px auto;padding:0 16px'>" +
      "<div style='background:#182848;color:#fff;padding:20px 25px;border-radius:12px 12px 0 0;" +
      "font-size:22px;font-weight:bold'>LeaveHub</div>" +
      "<div style='background:#fff;padding:28px;border:1px solid #e4eaf3;border-top:0;border-radius:0 0 12px 12px'>" +
      "<h1 style='font-size:21px;margin:0 0 20px'>" +
      escape(title) +
      "</h1>" +
      "<p style='line-height:1.65'>" +
      greeting +
      "</p>" +
      "<p style='line-height:1.65'>" +
      body +
      "</p>" +
      button +
      "<p style='color:#71809a;font-size:12px;line-height:1.6;margin-top:24px'>" +
      escape(footer) +
      "</p>" +
      "</div></div></body></html>"
    );
  }

  static String escape(String value) {
    if (value == null) return "";
    return value
      .replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&#39;");
  }
}
