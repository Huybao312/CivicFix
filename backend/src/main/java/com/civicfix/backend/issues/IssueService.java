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
public class IssueService {
  private final JdbcTemplate db;
  private final AccountAccess access;
  private final IssuePolicy policy;

  public IssueService(JdbcTemplate db, AccountAccess access, IssuePolicy policy) {
    this.db = db;
    this.access = access;
    this.policy = policy;
  }

  private boolean manager(UUID user, UUID building) {
    return access.hasRole(user, building, "MANAGER");
  }

  private boolean reader(UUID user, UUID building) {
    return manager(user, building) || access.hasRole(user, building, "RESIDENT");
  }

  private static String hash(String... values) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      for (String value : values) {
        digest.update(value.getBytes(StandardCharsets.UTF_8));
        digest.update((byte) 0);
      }
      return HexFormat.of().formatHex(digest.digest());
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private void notFound() {
    throw new Problem(404, "Không tìm thấy phiếu trong phạm vi của bạn.");
  }

  @Transactional
  public Map<String, Object> create(UUID user, UUID building, UUID key, IssueController.Create r) {
    // Same lock order as account lifecycle: user first. Revocation and snapshot creation serialize.
    db.queryForList("SELECT id FROM users WHERE id=? FOR UPDATE", user);
    if (!reader(user, building))
      throw new Problem(403, "Bạn không có quyền báo sự cố tại chung cư này.");
    String location = r.locationDescription().strip(), description = r.description().strip();
    String category = r.category() == null ? "OTHER" : r.category();
    if (!"COMMON_AREA".equals(r.scope()))
      throw new Problem(
          422,
          "CivicFix chỉ tiếp nhận sự cố ở khu vực chung. Hãy liên hệ đơn vị phụ trách thiết bị"
              + " riêng trong căn hộ.");
    if (location.length() < 3
        || description.length() < 10
        || !Set.of("OTHER", "ELECTRICAL", "WATER", "ELEVATOR").contains(category))
      throw new Problem(422, "Kiểm tra vị trí, mô tả và loại sự cố.");
    String fingerprint =
        hash(location, description, category, r.scope(), Objects.toString(r.apartmentLinkId(), ""));
    var previous =
        db.queryForList(
            "SELECT id,request_hash FROM issues WHERE building_id=? AND reported_by=? AND"
                + " idempotency_key=?",
            building,
            user,
            key);
    if (!previous.isEmpty()) {
      if (!fingerprint.equals(previous.getFirst().get("request_hash")))
        throw new Problem(
            409, "Yêu cầu này đã được gửi với nội dung khác. Hãy tải lại phiếu trước khi báo mới.");
      return detail(user, building, (UUID) previous.getFirst().get("id"));
    }
    var links =
        access.apartments(user).stream()
            .filter(a -> building.equals(a.get("building_id")))
            .toList();
    Map<String, Object> apartment = null;
    if (r.apartmentLinkId() != null) {
      apartment =
          links.stream()
              .filter(a -> r.apartmentLinkId().equals(a.get("link_id")))
              .findFirst()
              .orElseThrow(
                  () ->
                      new Problem(
                          422, "Căn hộ được chọn không còn thuộc liên kết đã xác minh của bạn."));
    } else if (links.size() == 1) apartment = links.getFirst();
    else if (links.size() > 1)
      throw new Problem(422, "Chọn một trong các căn hộ đã xác minh của bạn.");
    UUID id = UUID.randomUUID();
    String name =
        db.queryForObject("SELECT display_name FROM users WHERE id=?", String.class, user);
    db.update(
        """
        INSERT INTO issues(id,building_id,reported_by,reporter_name_at_report,apartment_id_at_report,
          apartment_label_at_report,location_description,description,category,idempotency_key,request_hash)
        VALUES (?,?,?,?,?,?,?,?,?,?,?)
        """,
        id,
        building,
        user,
        name,
        apartment == null ? null : apartment.get("id"),
        apartment == null ? null : apartment.get("unit_number"),
        location,
        description,
        category,
        key,
        fingerprint);
    db.update(
        "INSERT INTO issue_events(id,issue_id,actor_id,event_type,public_message) VALUES"
            + " (?,?,?,'ISSUE_SUBMITTED',?)",
        UUID.randomUUID(),
        id,
        user,
        "Đã ghi nhận báo cáo. Quản lý sẽ xem xét trước khi tiếp nhận xử lý.");
    return detail(user, building, id);
  }

  private static final String FIELDS =
      """
      i.id,i.code,i.building_id,i.reported_by,i.reporter_name_at_report,i.apartment_id_at_report,
      i.apartment_label_at_report,i.location_description,i.description,i.category,i.scope,i.source,i.status,i.version,i.created_at,i.priority,i.question
      """;

  public Map<String, Object> list(
      UUID user, UUID building, int page, int size, String q, String status, String priority) {
    var scope = policy.listScope(user, building);
    if (page < 0
        || page > 50000
        || size < 1
        || size > 50
        || q.length() > 120
        || status.length() > 24
        || priority.length() > 12) throw new Problem(422, "Tham số danh sách không hợp lệ.");
    if (!status.isEmpty()
        && !Set.of(
                "SUBMITTED",
                "NEEDS_INFO",
                "ACCEPTED",
                "ASSIGNED",
                "IN_PROGRESS",
                "ON_HOLD",
                "RESOLVED",
                "CLOSED",
                "REJECTED")
            .contains(status)) throw new Problem(422, "Trạng thái lọc không hợp lệ.");
    if (!priority.isEmpty() && !Set.of("LOW", "MEDIUM", "HIGH", "URGENT").contains(priority))
      throw new Problem(422, "Ưu tiên lọc không hợp lệ.");
    List<Object> args = new ArrayList<>();
    args.add(building);
    String where =
        " WHERE i.building_id=? AND "
            + scope.sql(args)
            + " AND position(lower(?) in lower(i.code || ' ' || i.location_description || ' ' ||"
            + " i.description))>0";
    args.add(q.strip());
    if (!status.isEmpty()) {
      where += " AND i.status=?";
      args.add(status);
    }
    if (!priority.isEmpty()) {
      where += " AND i.priority=?";
      args.add(priority);
    }
    args.add(size + 1);
    args.add(page * size);
    String order =
        scope.manager()
            ? "CASE i.status WHEN 'SUBMITTED' THEN 0 WHEN 'NEEDS_INFO' THEN 1 WHEN 'ACCEPTED' THEN"
                + " 2 WHEN 'ASSIGNED' THEN 3 ELSE 4 END, CASE i.priority WHEN 'URGENT' THEN 0"
                + " WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END,"
            : "";
    var rows =
        db.queryForList(
            "SELECT "
                + FIELDS
                + " FROM issues i"
                + where
                + " ORDER BY "
                + order
                + " i.created_at DESC,i.id DESC LIMIT ? OFFSET ?",
            args.toArray());
    boolean more = rows.size() > size;
    var visible = (more ? rows.subList(0, size) : rows).stream().map(scope::project).toList();
    return Map.of("items", visible, "page", page, "size", size, "hasMore", more);
  }

  public Map<String, Object> detail(UUID user, UUID building, UUID id) {
    var scope = policy.scope(user, building);
    List<Object> args = new ArrayList<>(List.of(id, building));
    var rows =
        db.queryForList(
            "SELECT "
                + FIELDS
                + " FROM issues i WHERE i.id=? AND i.building_id=? AND "
                + scope.sql(args),
            args.toArray());
    if (rows.isEmpty()) notFound();
    var row = rows.getFirst();
    var result = scope.project(row);
    boolean reporter = (scope.resident() || scope.manager()) && user.equals(row.get("reported_by"));
    result.put(
        "permissions",
        Map.of(
            "manage",
            scope.manager(),
            "provideInfo",
            reporter && "NEEDS_INFO".equals(row.get("status"))));
    if (scope.manager() || scope.technician()) {
      List<Object> assignmentArgs = new ArrayList<>(List.of(id));
      String assignmentScope = "";
      if (!scope.manager()) {
        assignmentScope = " AND a.technician_id=?";
        assignmentArgs.add(user);
      }
      var assignments =
          db.queryForList(
              "SELECT a.id,a.technician_id,u.display_name AS technician_name FROM issue_assignments"
                  + " a JOIN users u ON u.id=a.technician_id WHERE a.issue_id=? AND a.ended_at IS"
                  + " NULL"
                  + assignmentScope,
              assignmentArgs.toArray());
      result.put("assignment", assignments.isEmpty() ? null : assignments.getFirst());
    }
    return result;
  }

  public List<Map<String, Object>> events(UUID user, UUID building, UUID id) {
    detail(user, building, id);
    boolean manager = access.hasRole(user, building, "MANAGER");
    return db.queryForList(
        "SELECT e.id,e.event_type,e.public_message,e.created_at"
            + (manager ? ",e.actor_id,u.display_name AS actor_name" : "")
            + " FROM issue_events e JOIN users u ON u.id=e.actor_id WHERE e.issue_id=?"
            + (manager ? "" : " AND e.visibility='PUBLIC'")
            + " ORDER BY e.created_at,e.event_order",
        id);
  }
}
