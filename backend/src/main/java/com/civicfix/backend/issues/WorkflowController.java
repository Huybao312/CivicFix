package com.civicfix.backend.issues;

import com.civicfix.backend.accounts.AccountAccess;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/buildings/{building}/issues")
public class WorkflowController {
  private final AccountAccess access;
  private final IssueWorkflow workflow;

  public WorkflowController(AccountAccess access, IssueWorkflow workflow) {
    this.access = access;
    this.workflow = workflow;
  }

  public record Command(
      @NotNull @Min(0) Long expectedVersion,
      @Size(max = 800) String text,
      @Size(max = 12) String priority,
      UUID technicianId) {
    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
      throw new IllegalArgumentException("Unexpected field");
    }
  }

  @GetMapping("/technicians")
  Object technicians(@PathVariable UUID building, Authentication auth) {
    return workflow.technicians(access.user(auth), building);
  }

  @PostMapping("/{id}/actions/{action}")
  Object apply(
      @PathVariable UUID building,
      @PathVariable UUID id,
      @PathVariable String action,
      Authentication auth,
      @RequestHeader("Idempotency-Key") UUID key,
      @Valid @RequestBody Command command) {
    return workflow.apply(access.user(auth), building, id, action, key, command);
  }
}
