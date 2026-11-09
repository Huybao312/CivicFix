package com.civicfix.backend.accounts;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiErrors {
  public static class Problem extends RuntimeException {
    final int status;

    public Problem(int status, String message) {
      super(message);
      this.status = status;
    }
  }

  @ExceptionHandler(Problem.class)
  ResponseEntity<?> problem(Problem e) {
    return ResponseEntity.status(e.status).body(Map.of("message", e.getMessage()));
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    IllegalArgumentException.class,
    ServletRequestBindingException.class,
    MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<?> validation(Exception e) {
    return ResponseEntity.unprocessableContent()
        .body(Map.of("message", "Thông tin không hợp lệ. Kiểm tra lại các trường đã nhập."));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  ResponseEntity<?> conflict() {
    return ResponseEntity.status(409)
        .body(Map.of("message", "Dữ liệu vừa thay đổi hoặc đã tồn tại. Hãy tải lại và thử lại."));
  }
}
