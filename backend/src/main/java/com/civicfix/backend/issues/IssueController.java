package com.civicfix.backend.issues;

import com.civicfix.backend.accounts.AccountAccess;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/buildings/{building}/issues")
public class IssueController {
  private final IssueService issues;
  private final AccountAccess access;

  public IssueController(IssueService issues, AccountAccess access) {
    this.issues = issues;
    this.access = access;
  }

  public record Create(
      @NotBlank @Size(min = 3, max = 300) String locationDescription,
      @NotBlank @Size(min = 10, max = 3000) String description,
      @NotBlank String scope,
      @Size(max = 20) String category,
      UUID apartmentLinkId) {
    @JsonAnySetter
    public void rejectUnknown(String name, Object value) {
      throw new IllegalArgumentException("Unexpected field");
    }
  }

  @PostMapping
  Map<String, Object> create(
      @PathVariable UUID building,
      Authentication auth,
      @RequestHeader("Idempotency-Key") UUID key,
      @Valid @RequestBody Create r) {
    return issues.create(access.user(auth), building, key, r);
  }

  @GetMapping
  Map<String, Object> list(
      @PathVariable UUID building,
      Authentication auth,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String status,
      @RequestParam(defaultValue = "") String priority) {
    return issues.list(access.user(auth), building, page, size, q, status, priority);
  }

  @GetMapping("/{id}")
  Map<String, Object> detail(
      @PathVariable UUID building, @PathVariable UUID id, Authentication auth) {
    return issues.detail(access.user(auth), building, id);
  }

  @GetMapping("/{id}/events")
  Object history(@PathVariable UUID building, @PathVariable UUID id, Authentication auth) {
    return issues.events(access.user(auth), building, id);
  }
}
