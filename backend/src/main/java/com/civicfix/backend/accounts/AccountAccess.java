package com.civicfix.backend.accounts;

import static com.civicfix.backend.accounts.ApiErrors.Problem;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AccountAccess {
  final JdbcTemplate db;

  public AccountAccess(JdbcTemplate db) {
    this.db = db;
  }

  public UUID user(Authentication auth) {
    if (auth == null || !(auth.getPrincipal() instanceof AccountPrincipal p))
      throw new Problem(401, "Vui lòng đăng nhập.");
    return p.id();
  }

  public boolean hasRole(UUID user, UUID building, String role) {
    return Boolean.TRUE.equals(
        db.queryForObject(
            """
            SELECT EXISTS(SELECT 1 FROM building_memberships m JOIN users u ON u.id=m.user_id
             WHERE m.user_id=? AND m.building_id=? AND m.role=? AND m.status='ACTIVE'
             AND u.status='ACTIVE' AND m.valid_from<=CURRENT_TIMESTAMP
             AND (m.valid_to IS NULL OR m.valid_to>CURRENT_TIMESTAMP))
            """,
            Boolean.class,
            user,
            building,
            role));
  }

  public void admin(UUID user, UUID building) {
    if (!hasRole(user, building, "ADMIN"))
      throw new Problem(403, "Bạn không có quyền quản trị tại chung cư này.");
  }

  public List<Map<String, Object>> memberships(UUID user) {
    return db.queryForList(
        """
        SELECT m.id, m.building_id, b.name AS building_name, m.role, m.valid_from, m.valid_to
        FROM building_memberships m JOIN buildings b ON b.id=m.building_id
        WHERE m.user_id=? AND m.status='ACTIVE' AND m.valid_from<=CURRENT_TIMESTAMP
          AND (m.valid_to IS NULL OR m.valid_to>CURRENT_TIMESTAMP)
        ORDER BY b.name,m.role
        """,
        user);
  }

  public List<Map<String, Object>> apartments(UUID user) {
    return db.queryForList(
        """
        SELECT l.id AS link_id,a.id,a.building_id,a.unit_number,a.floor,b.name AS building_name
        FROM resident_apartment_links l JOIN apartments a ON a.id=l.apartment_id
        JOIN buildings b ON b.id=a.building_id
        WHERE l.user_id=? AND l.status='VERIFIED' AND l.valid_from<=CURRENT_TIMESTAMP
          AND (l.valid_to IS NULL OR l.valid_to>CURRENT_TIMESTAMP)
          AND EXISTS(SELECT 1 FROM building_memberships m WHERE m.user_id=l.user_id
            AND m.building_id=a.building_id AND m.role='RESIDENT' AND m.status='ACTIVE'
            AND m.valid_from<=CURRENT_TIMESTAMP AND (m.valid_to IS NULL OR m.valid_to>CURRENT_TIMESTAMP))
        ORDER BY b.name,a.unit_number
        """,
        user);
  }

  public void audit(UUID actor, UUID building, String action, UUID target, String reason) {
    db.update(
        "INSERT INTO account_audit_events(id,actor_id,building_id,action,target_id,reason) VALUES"
            + " (?,?,?,?,?,?)",
        UUID.randomUUID(),
        actor,
        building,
        action,
        target,
        reason);
  }

  public void invalidate(UUID user) {
    db.update("UPDATE users SET auth_version=auth_version+1 WHERE id=?", user);
  }
}
