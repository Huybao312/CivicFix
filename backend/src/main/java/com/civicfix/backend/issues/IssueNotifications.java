package com.civicfix.backend.issues;

import com.civicfix.backend.accounts.AccountAccess;
import com.civicfix.backend.accounts.ApiErrors.Problem;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/buildings/{building}/notifications")
public class IssueNotifications {
  private final JdbcTemplate db;
  private final AccountAccess access;
  private final IssuePolicy policy;
  private final IssueService issues;

  public IssueNotifications(
      JdbcTemplate db, AccountAccess access, IssuePolicy policy, IssueService issues) {
    this.db = db;
    this.access = access;
    this.policy = policy;
    this.issues = issues;
  }

  @GetMapping
  Object list(@PathVariable UUID building, Authentication auth) {
    UUID user = access.user(auth);
    var scope = policy.listScope(user, building);
    List<Object> args = new ArrayList<>(List.of(user, building));
    return db.queryForList(
        "SELECT n.id,n.issue_id,n.message,n.read_at,n.created_at FROM issue_notifications n JOIN"
            + " issues i ON i.id=n.issue_id WHERE n.recipient_id=? AND n.building_id=? AND "
            + scope.sql(args)
            + " ORDER BY n.created_at DESC,n.id DESC LIMIT 100",
        args.toArray());
  }

  @PostMapping("/{id}/read")
  @Transactional
  Object read(@PathVariable UUID building, @PathVariable UUID id, Authentication auth) {
    UUID user = access.user(auth);
    var rows =
        db.queryForList(
            "SELECT issue_id FROM issue_notifications WHERE id=? AND building_id=? AND"
                + " recipient_id=?",
            id,
            building,
            user);
    if (rows.isEmpty()) throw new Problem(404, "Không tìm thấy thông báo.");
    issues.detail(user, building, (UUID) rows.getFirst().get("issue_id"));
    db.update(
        "UPDATE issue_notifications SET read_at=COALESCE(read_at,CURRENT_TIMESTAMP) WHERE id=? AND"
            + " recipient_id=?",
        id,
        user);
    return Map.of("ok", true);
  }
}
