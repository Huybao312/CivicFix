package com.civicfix.backend.issues;

import com.civicfix.backend.accounts.AccountAccess;
import com.civicfix.backend.accounts.ApiErrors.Problem;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IssueWorkflow {
  private final JdbcTemplate db;
  private final AccountAccess access;
  private final IssueService issues;
  private static final Set<String> ACTIONS =
      Set.of(
          "ACCEPT", "REQUEST_INFO", "PROVIDE_INFO", "REJECT", "RESTORE", "SET_PRIORITY", "ASSIGN");
  private static final Set<String> PRIORITIES = Set.of("LOW", "MEDIUM", "HIGH", "URGENT");

  public IssueWorkflow(JdbcTemplate db, AccountAccess access, IssueService issues) {
    this.db = db;
    this.access = access;
    this.issues = issues;
  }

  public static class VersionConflict extends Problem {
    public final long currentVersion;
    public final String currentStatus;

    public VersionConflict(Map<String, Object> issue) {
      super(409, "Phiếu vừa được cập nhật. Hãy tải lại và kiểm tra trước khi quyết định.");
      currentVersion = ((Number) issue.get("version")).longValue();
      currentStatus = (String) issue.get("status");
    }
  }

  private String fingerprint(String action, WorkflowController.Command command) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      for (String value :
          List.of(
              action,
              command.expectedVersion().toString(),
              Objects.toString(command.text(), "").strip(),
              Objects.toString(command.priority(), ""),
              Objects.toString(command.technicianId(), ""))) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @Transactional
  public Map<String, Object> apply(
      UUID actor,
      UUID building,
      UUID id,
      String action,
      UUID key,
      WorkflowController.Command command) {
    if (!ACTIONS.contains(action)) throw new Problem(422, "Hành động không hợp lệ.");
    // Establish object access before accepting a command or exposing its current version.
    issues.detail(actor, building, id);
    authorize(actor, building, id, action);
    List<UUID> users = new ArrayList<>();
    users.add(actor);
    if (action.equals("ASSIGN") && command.technicianId() != null)
      users.add(command.technicianId());
    users = users.stream().distinct().sorted().toList();
    for (UUID user : users) db.queryForList("SELECT id FROM users WHERE id=? FOR UPDATE", user);
    // Account revocation uses the same user locks. Recheck permissions after acquiring them.
    authorize(actor, building, id, action);
    var rows =
        db.queryForList(
            "SELECT * FROM issues WHERE id=? AND building_id=? FOR UPDATE", id, building);
    if (rows.isEmpty()) throw new Problem(404, "Không tìm thấy phiếu trong phạm vi của bạn.");
    authorize(actor, building, id, action);
    var issue = rows.getFirst();
    String hash = fingerprint(action, command);
    var receipts =
        db.queryForList(
            "SELECT request_hash FROM issue_commands WHERE issue_id=? AND actor_id=? AND"
                + " idempotency_key=?",
            id,
            actor,
            key);
    if (!receipts.isEmpty()) {
      if (!hash.equals(receipts.getFirst().get("request_hash")))
        throw new Problem(409, "Mã yêu cầu đã được dùng với nội dung khác.");
      return issues.detail(actor, building, id);
    }
    if (((Number) issue.get("version")).longValue() != command.expectedVersion())
      throw new VersionConflict(issue);
    String text = Objects.toString(command.text(), "").strip();
    if (text.length() < 3 || text.length() > 800)
      throw new Problem(422, "Nhập nội dung hoặc lý do từ 3 đến 800 ký tự.");
    String status = (String) issue.get("status");
    String nextStatus = status;
    String question = (String) issue.get("question");
    String priority = (String) issue.get("priority");
    String message;
    switch (action) {
      case "ACCEPT" -> {
        requireState(status, Set.of("SUBMITTED"));
        nextStatus = "ACCEPTED";
        message = "Đã tiếp nhận báo cáo. " + text;
      }
      case "REQUEST_INFO" -> {
        requireState(status, Set.of("SUBMITTED"));
        nextStatus = "NEEDS_INFO";
        question = text;
        message = "Quản lý yêu cầu bổ sung: " + text;
      }
      case "PROVIDE_INFO" -> {
        requireState(status, Set.of("NEEDS_INFO"));
        nextStatus = "SUBMITTED";
        question = null;
        message = "Người báo bổ sung: " + text;
      }
      case "REJECT" -> {
        requireState(status, Set.of("SUBMITTED", "NEEDS_INFO", "ACCEPTED"));
        nextStatus = "REJECTED";
        question = null;
        message = "Không tiếp nhận báo cáo. Lý do: " + text;
      }
      case "RESTORE" -> {
        requireState(status, Set.of("REJECTED"));
        nextStatus = "SUBMITTED";
        question = null;
        message = "Đã khôi phục báo cáo để xem xét. Lý do: " + text;
      }
      case "SET_PRIORITY" -> {
        requireState(
            status,
            Set.of("SUBMITTED", "NEEDS_INFO", "ACCEPTED", "ASSIGNED", "IN_PROGRESS", "ON_HOLD"));
        if (!PRIORITIES.contains(Objects.toString(command.priority(), "")))
          throw new Problem(422, "Chọn mức ưu tiên hợp lệ.");
        if (priority.equals(command.priority()))
          throw new Problem(422, "Chọn mức ưu tiên khác mức hiện tại.");
        message = "Ưu tiên " + priority + " → " + command.priority() + ". Lý do: " + text;
        priority = command.priority();
      }
      case "ASSIGN" -> {
        requireState(status, Set.of("ACCEPTED"));
        if (command.technicianId() == null
            || !access.hasRole(command.technicianId(), building, "TECHNICIAN"))
          throw new Problem(422, "Kỹ thuật viên cần có quyền đang hiệu lực tại chung cư này.");
        db.update(
            "INSERT INTO issue_assignments(id,issue_id,technician_id,assigned_by) VALUES (?,?,?,?)",
            UUID.randomUUID(),
            id,
            command.technicianId(),
            actor);
        nextStatus = "ASSIGNED";
        message = "Đã phân công kỹ thuật viên. " + text;
      }
      default -> throw new Problem(422, "Hành động không hợp lệ.");
    }
    db.update(
        "UPDATE issues SET status=?,priority=?,question=?,version=version+1 WHERE id=?",
        nextStatus,
        priority,
        question,
        id);
    UUID event = UUID.randomUUID();
    db.update(
        "INSERT INTO issue_events(id,issue_id,actor_id,event_type,public_message,visibility) VALUES"
            + " (?,?,?,?,?,?)",
        event,
        id,
        actor,
        action,
        message,
        action.equals("SET_PRIORITY") ? "INTERNAL" : "PUBLIC");
    notifyRecipients(building, issue, action, command.technicianId(), event);
    db.update(
        "INSERT INTO issue_commands(issue_id,actor_id,idempotency_key,request_hash,result_version)"
            + " VALUES (?,?,?,?,?)",
        id,
        actor,
        key,
        hash,
        command.expectedVersion() + 1);
    return issues.detail(actor, building, id);
  }

  private void authorize(UUID actor, UUID building, UUID id, String action) {
    if (action.equals("PROVIDE_INFO")) {
      if (!(access.hasRole(actor, building, "RESIDENT")
          || access.hasRole(actor, building, "MANAGER")))
        throw new Problem(403, "Bạn không có quyền bổ sung phiếu này.");
      boolean owner =
          Boolean.TRUE.equals(
              db.queryForObject(
                  "SELECT EXISTS(SELECT 1 FROM issues WHERE id=? AND building_id=? AND"
                      + " reported_by=?)",
                  Boolean.class,
                  id,
                  building,
                  actor));
      if (!owner) throw new Problem(403, "Chỉ người báo được bổ sung thông tin qua web.");
    } else if (!access.hasRole(actor, building, "MANAGER")) {
      throw new Problem(403, "Bạn không có quyền điều phối phiếu này.");
    }
  }

  private void requireState(String status, Set<String> allowed) {
    if (!allowed.contains(status))
      throw new Problem(409, "Trạng thái hiện tại không cho phép thao tác này. Hãy tải lại phiếu.");
  }

  private void notifyRecipients(
      UUID building, Map<String, Object> issue, String action, UUID technician, UUID event) {
    Set<UUID> recipients = new HashSet<>();
    String text;
    if (action.equals("PROVIDE_INFO")) {
      recipients.addAll(
          db.query(
              """
              SELECT DISTINCT m.user_id FROM building_memberships m JOIN users u ON u.id=m.user_id
              WHERE m.building_id=? AND m.role='MANAGER' AND m.status='ACTIVE' AND u.status='ACTIVE'
                AND m.valid_from<=CURRENT_TIMESTAMP AND (m.valid_to IS NULL OR m.valid_to>CURRENT_TIMESTAMP)
              """,
              (rs, n) -> rs.getObject(1, UUID.class),
              building));
      text = "Người báo đã bổ sung thông tin cho phiếu " + issue.get("code") + ".";
    } else if (action.equals("ASSIGN")) {
      recipients.add(technician);
      recipients.add((UUID) issue.get("reported_by"));
      text = "Phiếu " + issue.get("code") + " đã được phân công kỹ thuật viên.";
    } else if (action.equals("SET_PRIORITY")) {
      return;
    } else {
      recipients.add((UUID) issue.get("reported_by"));
      text =
          switch (action) {
                case "REQUEST_INFO" -> "Quản lý cần bạn bổ sung thông tin cho phiếu ";
                case "ACCEPT" -> "Quản lý đã tiếp nhận phiếu ";
                case "REJECT" -> "Quản lý đã từ chối tiếp nhận phiếu ";
                default -> "Quản lý đã khôi phục phiếu ";
              }
              + issue.get("code")
              + ".";
    }
    for (UUID recipient : recipients) {
      db.update(
          "INSERT INTO issue_notifications(id,recipient_id,building_id,issue_id,event_id,message)"
              + " VALUES (?,?,?,?,?,?)",
          UUID.randomUUID(),
          recipient,
          building,
          issue.get("id"),
          event,
          action.equals("ASSIGN") && recipient.equals(technician)
              ? "Bạn được giao phiếu " + issue.get("code") + "."
              : text);
    }
  }

  public List<Map<String, Object>> technicians(UUID actor, UUID building) {
    if (!access.hasRole(actor, building, "MANAGER"))
      throw new Problem(403, "Bạn không có quyền phân công tại chung cư này.");
    return db.queryForList(
        """
        SELECT DISTINCT u.id,u.display_name FROM users u JOIN building_memberships m ON m.user_id=u.id
        WHERE m.building_id=? AND m.role='TECHNICIAN' AND m.status='ACTIVE' AND u.status='ACTIVE'
          AND m.valid_from<=CURRENT_TIMESTAMP AND (m.valid_to IS NULL OR m.valid_to>CURRENT_TIMESTAMP)
        ORDER BY u.display_name,u.id
        """,
        building);
  }
}
