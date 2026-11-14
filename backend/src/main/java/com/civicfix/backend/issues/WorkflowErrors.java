package com.civicfix.backend.issues;

import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class WorkflowErrors {
  @ExceptionHandler(IssueWorkflow.VersionConflict.class)
  ResponseEntity<?> conflict(IssueWorkflow.VersionConflict error) {
    return ResponseEntity.status(409)
        .body(
            Map.of(
                "message",
                error.getMessage(),
                "currentVersion",
                error.currentVersion,
                "currentStatus",
                error.currentStatus));
  }
}
