package com.civicfix.backend.accounts;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AccountController {
  final AccountAccess access;
  final JdbcTemplate db;
  final AccountLifecycle lifecycle;

  public AccountController(AccountAccess access, JdbcTemplate db, AccountLifecycle lifecycle) {
    this.access = access;
    this.db = db;
    this.lifecycle = lifecycle;
  }

  @GetMapping("/auth/csrf")
  Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  @GetMapping("/auth/me")
  Map<String, Object> me(Authentication auth) {
    UUID user = access.user(auth);
    var info =
        new LinkedHashMap<>(
            db.queryForMap("SELECT id,email,display_name FROM users WHERE id=?", user));
    info.put("memberships", access.memberships(user));
    info.put("apartments", access.apartments(user));
    return info;
  }

  record Email(@NotBlank @jakarta.validation.constraints.Email @Size(max = 254) String email) {}

  record Reset(
      @NotBlank @Size(max = 100) String token, @NotNull @Size(max = 512) String password) {}

  record Token(@NotBlank @Size(max = 100) String token) {}

  record Accept(@NotBlank @Size(max = 100) String token, @Size(max = 512) String password) {}

  @PostMapping("/auth/forgot-password")
  Map<String, String> forgot(@Valid @RequestBody Email email, HttpServletRequest req) {
    lifecycle.forgot(email.email(), req.getRemoteAddr());
    return Map.of(
        "message", "Nếu email khớp với tài khoản đủ điều kiện, chúng tôi sẽ gửi hướng dẫn.");
  }

  @PostMapping("/auth/reset-password")
  Map<String, Boolean> reset(@Valid @RequestBody Reset r) {
    lifecycle.reset(r.token(), r.password());
    return Map.of("ok", true);
  }

  @PostMapping("/invitations/preview")
  Map<String, Object> preview(@Valid @RequestBody Token t, HttpServletRequest req) {
    lifecycle.publicLimit(req.getRemoteAddr());
    return lifecycle.preview(t.token());
  }

  @PostMapping("/invitations/accept")
  Map<String, Boolean> accept(
      @Valid @RequestBody Accept a, Authentication auth, HttpServletRequest req) {
    lifecycle.publicLimit(req.getRemoteAddr());
    UUID actor = auth != null && auth.getPrincipal() instanceof AccountPrincipal p ? p.id() : null;
    lifecycle.accept(a.token(), a.password(), actor);
    return Map.of("ok", true);
  }

  @PostMapping("/invitations/report-incorrect")
  Map<String, Boolean> incorrect(@Valid @RequestBody Token t, HttpServletRequest req) {
    lifecycle.publicLimit(req.getRemoteAddr());
    lifecycle.incorrect(t.token());
    return Map.of("ok", true);
  }
}
