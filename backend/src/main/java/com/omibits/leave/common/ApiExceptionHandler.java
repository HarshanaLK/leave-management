package com.omibits.leave.common;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class ApiExceptionHandler {

  private ResponseEntity<Map<String, Object>> error(
    HttpStatus status,
    String message
  ) {
    return ResponseEntity.status(status).body(
      Map.of(
        "error",
        message,
        "status",
        status.value(),
        "timestamp",
        Instant.now().toString()
      )
    );
  }

  @ExceptionHandler(ApiException.class)
  ResponseEntity<Map<String, Object>> api(ApiException e) {
    return error(e.status(), e.getMessage());
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String, Object>> validation(
    MethodArgumentNotValidException e
  ) {
    var fields = new LinkedHashMap<String, String>();
    e.getBindingResult()
      .getFieldErrors()
      .forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
    return ResponseEntity.badRequest().body(
      Map.of("error", "Validation failed", "fields", fields, "status", 400)
    );
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  ResponseEntity<Map<String, Object>> parameterValidation(
    HandlerMethodValidationException e
  ) {
    return error(
      HttpStatus.BAD_REQUEST,
      "Invalid query parameter: check field lengths, IDs, and ranges"
    );
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  ResponseEntity<Map<String, Object>> parameterType(
    MethodArgumentTypeMismatchException e
  ) {
    return error(
      HttpStatus.BAD_REQUEST,
      "Invalid value for query parameter: " + e.getName()
    );
  }

  @ExceptionHandler({
    ConstraintViolationException.class,
    HttpMessageNotReadableException.class,
    IllegalArgumentException.class,
  })
  ResponseEntity<Map<String, Object>> invalid(Exception e) {
    return error(HttpStatus.BAD_REQUEST, "Invalid request parameters or body");
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<Map<String, Object>> conflict(
    DataIntegrityViolationException e
  ) {
    return error(
      HttpStatus.CONFLICT,
      "A duplicate or invalid related record exists"
    );
  }

  @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
  ResponseEntity<Map<String, Object>> race(Exception e) {
    return error(
      HttpStatus.CONFLICT,
      "Request changed concurrently; refresh and retry"
    );
  }

  @ExceptionHandler(AuthenticationException.class)
  ResponseEntity<Map<String, Object>> badCredentials(
    AuthenticationException e
  ) {
    return error(HttpStatus.UNAUTHORIZED, "Invalid email or password");
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<Map<String, Object>> large(Exception e) {
    return error(HttpStatus.PAYLOAD_TOO_LARGE, "Maximum file size is 5 MB");
  }
}
