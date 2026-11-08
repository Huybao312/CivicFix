package com.civicfix.backend.accounts;

import static com.civicfix.backend.accounts.ApiErrors.Problem;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/buildings/{building}")
public class AdminController {
  final JdbcTemplate db;
  final AccountAccess access;
  final AccountLifecycle lifecycle;

  public AdminController(JdbcTemplate db, AccountAccess access, AccountLifecycle lifecycle) {
    this.db = db;
    this.access = access;
    this.lifecycle = lifecycle;
  }

  public record Invite(
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(max = 160) String displayName,
      @NotNull UUID apartmentId,
      @NotBlank @Size(max = 160) String verificationSource,
      OffsetDateTime validFrom) {}

  record Reason(@NotBlank @Size(max = 500) String reason) {}

  record Link(
      @NotNull UUID userId,
      @NotNull UUID apartmentId,
      @NotBlank @Size(max = 160) String verificationSource) {}

  @GetMapping("/accounts")
  List<Map<String, Object>> accounts(@PathVariable UUID building, Authentication auth) {
    access.admin(access.user(auth), building);
    return db.queryForList(
        """
        SELECT m.id AS membership_id,u.id,u.display_name,u.email,u.status AS account_status,
            m.role,m.status AS membership_status,m.valid_from,m.valid_to,
            (u.status='ACTIVE' AND m.status='ACTIVE' AND m.valid_from<=CURRENT_TIMESTAMP
                AND (m.valid_to IS NULL OR m.valid_to>CURRENT_TIMESTAMP)) AS effective,
            COALESCE((SELECT string_agg(a.unit_number,', ' ORDER BY a.unit_number)
                FROM resident_apartment_links l JOIN apartments a ON a.id=l.apartment_id
                WHERE l.user_id=u.id AND a.building_id=m.building_id AND l.status='VERIFIED'
                 AND l.valid_from<=CURRENT_TIMESTAMP AND (l.valid_to IS NULL OR l.valid_to>CURRENT_TIMESTAMP)),'') AS apartments
        FROM building_memberships m JOIN users u ON u.id=m.user_id WHERE m.building_id=?
        ORDER BY u.display_name,m.created_at DESC
        """,
        building);
  }

  @GetMapping("/apartments")
  List<Map<String, Object>> apartments(@PathVariable UUID building, Authentication auth) {
    access.admin(access.user(auth), building);
    return db.queryForList(
        "SELECT id,unit_number,floor FROM apartments WHERE building_id=? ORDER BY unit_number",
        building);
  }

  @GetMapping("/invitations")
  List<Map<String, Object>> invitations(@PathVariable UUID building, Authentication auth) {
    access.admin(access.user(auth), building);
    return db.queryForList(
        """
        SELECT i.id,i.user_id,u.email,u.display_name,a.unit_number,i.status,i.expires_at,i.last_sent_at,
            i.incorrect_details,i.reason, (i.status='PENDING' AND i.expires_at<=CURRENT_TIMESTAMP) AS expired
        FROM account_invitations i JOIN users u ON u.id=i.user_id JOIN apartments a ON a.id=i.apartment_id
        WHERE i.building_id=? ORDER BY i.created_at DESC LIMIT 200
        """,
        building);
  }

  @PostMapping("/invitations")
  Map<String, Object> invite(
      @PathVariable UUID building, Authentication auth, @Valid @RequestBody Invite r) {
    return lifecycle.invite(access.user(auth), building, r);
  }

  @PostMapping("/invitations/{id}/resend")
  Map<String, Boolean> resend(
      @PathVariable UUID building, @PathVariable UUID id, Authentication auth) {
    lifecycle.resend(access.user(auth), building, id);
    return Map.of("ok", true);
  }

  @PostMapping("/invitations/{id}/revoke")
  Map<String, Boolean> revoke(
      @PathVariable UUID building,
      @PathVariable UUID id,
      Authentication auth,
      @Valid @RequestBody Reason r) {
    lifecycle.revoke(access.user(auth), building, id, r.reason());
    return Map.of("ok", true);
  }

  @PostMapping("/memberships/{id}/end")
  @Transactional
  Map<String, Boolean> end(
      @PathVariable UUID building,
      @PathVariable UUID id,
      Authentication auth,
      @Valid @RequestBody Reason reason) {
    UUID actor = access.user(auth);
    access.admin(actor, building);
    var initial =
        db.queryForList(
            "SELECT user_id FROM building_memberships WHERE id=? AND building_id=?", id, building);
    if (initial.isEmpty()) throw new Problem(404, "Không tìm thấy membership.");
    db.queryForList(
        "SELECT id FROM users WHERE id=? FOR UPDATE", initial.getFirst().get("user_id"));
    var rows =
        db.queryForList(
            "SELECT * FROM building_memberships WHERE id=? AND building_id=? FOR UPDATE",
            id,
            building);
    if (rows.isEmpty()) throw new Problem(404, "Không tìm thấy membership.");
    var m = rows.getFirst();
    UUID user = (UUID) m.get("user_id");
    if (actor.equals(user) && "ADMIN".equals(m.get("role")))
      throw new Problem(409, "Không tự kết thúc quyền quản trị của phiên hiện tại.");
    if ("ENDED".equals(m.get("status"))) return Map.of("ok", true);
    db.update("UPDATE building_memberships SET status='ENDED' WHERE id=?", id);
    db.update(
        "UPDATE account_invitations SET status='REVOKED',revoked_at=CURRENT_TIMESTAMP,reason=?"
            + " WHERE membership_id=? AND status='PENDING'",
        reason.reason(),
        id);
    if ("RESIDENT".equals(m.get("role"))) endLinks(user, building);
    access.invalidate(user);
    access.audit(actor, building, "MEMBERSHIP_ENDED", id, reason.reason());
    return Map.of("ok", true);
  }

  private void endLinks(UUID user, UUID building) {
    db.update(
        """
        UPDATE resident_apartment_links SET status='ENDED',ended_at=CURRENT_TIMESTAMP
        WHERE user_id=? AND status='VERIFIED' AND apartment_id IN (SELECT id FROM apartments WHERE building_id=?)
        """,
        user,
        building);
  }

  @PostMapping("/accounts/{id}/enable")
  @Transactional
  Map<String, Boolean> enable(
      @PathVariable UUID building,
      @PathVariable UUID id,
      Authentication auth,
      @Valid @RequestBody Reason reason) {
    UUID actor = access.user(auth);
    access.admin(actor, building);
    db.queryForList("SELECT id FROM users WHERE id=? FOR UPDATE", id);
    if (!Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM building_memberships WHERE user_id=? AND building_id=?)",
            Boolean.class,
            id,
            building))) throw new Problem(404, "Không tìm thấy tài khoản trong chung cư.");
    if (Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM building_memberships WHERE user_id=? AND building_id<>?"
                + " AND status<>'ENDED')",
            Boolean.class,
            id,
            building)))
      throw new Problem(
          409, "Tài khoản còn quyền ở chung cư khác; không thể thay đổi trạng thái toàn cục.");
    db.update(
        """
        UPDATE users SET status=CASE WHEN password_hash IS NOT NULL AND email_verified_at IS NOT NULL
            THEN 'ACTIVE' ELSE 'INVITED' END,auth_version=auth_version+1 WHERE id=? AND status='DISABLED'
        """,
        id);
    access.audit(actor, building, "ACCOUNT_ENABLED", id, reason.reason());
    return Map.of("ok", true);
  }

  @PostMapping("/accounts/{id}/disable")
  @Transactional
  Map<String, Boolean> disable(
      @PathVariable UUID building,
      @PathVariable UUID id,
      Authentication auth,
      @Valid @RequestBody Reason reason) {
    UUID actor = access.user(auth);
    access.admin(actor, building);
    db.queryForList("SELECT id FROM users WHERE id=? FOR UPDATE", id);
    if (actor.equals(id))
      throw new Problem(409, "Không tự vô hiệu hóa tài khoản trong phiên hiện tại.");
    if (!Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM building_memberships WHERE user_id=? AND building_id=?)",
            Boolean.class,
            id,
            building))) throw new Problem(404, "Không tìm thấy tài khoản trong chung cư.");
    if (Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM building_memberships WHERE user_id=? AND building_id<>?"
                + " AND status<>'ENDED')",
            Boolean.class,
            id,
            building)))
      throw new Problem(
          409,
          "Tài khoản còn quyền tại chung cư khác. Hãy kết thúc riêng membership của chung cư này.");
    db.update("UPDATE users SET status='DISABLED',auth_version=auth_version+1 WHERE id=?", id);
    db.update(
        "UPDATE password_reset_tokens SET used_at=CURRENT_TIMESTAMP WHERE user_id=? AND used_at IS"
            + " NULL",
        id);
    db.update(
        "UPDATE building_memberships SET status='ENDED' WHERE user_id=? AND building_id=?",
        id,
        building);
    endLinks(id, building);
    db.update(
        "UPDATE account_invitations SET status='REVOKED',revoked_at=CURRENT_TIMESTAMP,reason=?"
            + " WHERE user_id=? AND building_id=? AND status='PENDING'",
        reason.reason(),
        id,
        building);
    access.audit(actor, building, "ACCOUNT_DISABLED", id, reason.reason());
    return Map.of("ok", true);
  }

  @GetMapping("/accounts/{id}/links")
  List<Map<String, Object>> links(
      @PathVariable UUID building, @PathVariable UUID id, Authentication auth) {
    access.admin(access.user(auth), building);
    return db.queryForList(
        """
        SELECT l.id,l.status,l.valid_from,l.valid_to,l.ended_at,l.verified_at,l.verification_source,
            a.unit_number,v.display_name AS verified_by
        FROM resident_apartment_links l JOIN apartments a ON a.id=l.apartment_id JOIN users v ON v.id=l.verified_by
        WHERE a.building_id=? AND l.user_id=? ORDER BY l.created_at DESC
        """,
        building,
        id);
  }

  @PostMapping("/links")
  @Transactional
  Map<String, Boolean> link(
      @PathVariable UUID building, Authentication auth, @Valid @RequestBody Link r) {
    UUID actor = access.user(auth);
    access.admin(actor, building);
    if (!List.of("RESIDENT_LIST", "OPERATOR_CONFIRMATION").contains(r.verificationSource()))
      throw new Problem(422, "Chọn căn cứ xác minh hợp lệ.");
    if (!Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM apartments WHERE id=? AND building_id=?)",
            Boolean.class,
            r.apartmentId(),
            building))) throw new Problem(404, "Không tìm thấy căn hộ.");
    db.queryForList("SELECT id FROM users WHERE id=? FOR UPDATE", r.userId());
    if (!access.hasRole(r.userId(), building, "RESIDENT"))
      throw new Problem(409, "Cư dân cần có membership đang hiệu lực tại chung cư này.");
    UUID id = UUID.randomUUID();
    db.update(
        "INSERT INTO"
            + " resident_apartment_links(id,user_id,apartment_id,verified_by,verification_source)"
            + " VALUES (?,?,?,?,?)",
        id,
        r.userId(),
        r.apartmentId(),
        actor,
        r.verificationSource());
    access.audit(actor, building, "APARTMENT_VERIFIED", id, r.verificationSource());
    return Map.of("ok", true);
  }

  @PostMapping("/links/{id}/end")
  @Transactional
  Map<String, Boolean> endLink(
      @PathVariable UUID building,
      @PathVariable UUID id,
      Authentication auth,
      @Valid @RequestBody Reason reason) {
    UUID actor = access.user(auth);
    access.admin(actor, building);
    var initial =
        db.queryForList(
            "SELECT l.user_id FROM resident_apartment_links l JOIN apartments a ON"
                + " a.id=l.apartment_id WHERE l.id=? AND a.building_id=?",
            id,
            building);
    if (initial.isEmpty()) throw new Problem(404, "Không tìm thấy liên kết.");
    db.queryForList(
        "SELECT id FROM users WHERE id=? FOR UPDATE", initial.getFirst().get("user_id"));
    var rows =
        db.queryForList(
            "SELECT l.* FROM resident_apartment_links l JOIN apartments a ON a.id=l.apartment_id"
                + " WHERE l.id=? AND a.building_id=? FOR UPDATE OF l",
            id,
            building);
    if (rows.isEmpty()) throw new Problem(404, "Không tìm thấy liên kết.");
    if ("VERIFIED".equals(rows.getFirst().get("status"))) {
      db.update(
          "UPDATE resident_apartment_links SET status='ENDED',ended_at=CURRENT_TIMESTAMP WHERE"
              + " id=?",
          id);
      access.invalidate((UUID) rows.getFirst().get("user_id"));
      access.audit(actor, building, "APARTMENT_LINK_ENDED", id, reason.reason());
    }
    return Map.of("ok", true);
  }

  @GetMapping("/audit")
  List<Map<String, Object>> audit(@PathVariable UUID building, Authentication auth) {
    access.admin(access.user(auth), building);
    return db.queryForList(
        "SELECT id,actor_id,action,target_id,reason,created_at FROM account_audit_events WHERE"
            + " building_id=? ORDER BY created_at DESC LIMIT 100",
        building);
  }

  @GetMapping("/workspace/{role}")
  Map<String, String> workspace(
      @PathVariable UUID building, @PathVariable String role, Authentication auth) {
    UUID user = access.user(auth);
    if (!List.of("RESIDENT", "MANAGER", "TECHNICIAN").contains(role)
        || !access.hasRole(user, building, role))
      throw new Problem(403, "Bạn không có vai trò này tại chung cư.");
    return Map.of(
        "role",
        role,
        "message",
        "Tài khoản và quyền truy cập đã sẵn sàng. Chức năng phiếu sẽ được triển khai từ M3.");
  }
}
