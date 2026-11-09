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

  public IssueService(JdbcTemplate db, AccountAccess access) {
    this.db = db;
    this.access = access;
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
              .orElseThrow(() ->
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
      i.apartment_label_at_report,i.location_description,i.description,i.category,i.scope,i.source,i.status,i.version,i.created_at
      """;

  public Map<String, Object> list(UUID user, UUID building, int page, int size, String q) {
    if (!reader(user, building))
      throw new Problem(403, "Bạn không có quyền xem danh sách phiếu tại chung cư này.");
    if (page < 0 || page > 50000 || size < 1 || size > 50 || q.length() > 120)
      throw new Problem(422, "Tham số danh sách không hợp lệ.");
    boolean all = manager(user, building);
    String where =
        " WHERE i.building_id=?"
            + (all ? "" : " AND i.reported_by=?")
            + " AND position(lower(?) in lower(i.code || ' ' || i.location_description || ' ' ||"
            + " i.description))>0";
    List<Object> args = new ArrayList<>();
    args.add(building);
    if (!all) args.add(user);
    args.add(q.strip());
    args.add(size + 1);
    args.add(page * size);
    var rows =
        db.queryForList(
            "SELECT "
                + FIELDS
                + " FROM issues i"
                + where
                + " ORDER BY i.created_at DESC,i.id DESC LIMIT ? OFFSET ?",
            args.toArray());
    boolean more = rows.size() > size;
    return Map.of(
        "items", more ? rows.subList(0, size) : rows, "page", page, "size", size, "hasMore", more);
  }

  public Map<String, Object> detail(UUID user, UUID building, UUID id) {
    if (!reader(user, building)) notFound();
    var rows =
        db.queryForList(
            "SELECT "
                + FIELDS
                + " FROM issues i WHERE i.id=? AND i.building_id=?"
                + (manager(user, building) ? "" : " AND i.reported_by=?"),
            manager(user, building)
                ? new Object[] {id, building}
                : new Object[] {id, building, user});
    if (rows.isEmpty()) notFound();
    return rows.getFirst();
  }

  public List<Map<String, Object>> events(UUID user, UUID building, UUID id) {
    detail(user, building, id);
    return db.queryForList(
        "SELECT id,event_type,public_message,created_at FROM issue_events WHERE issue_id=? ORDER BY"
            + " created_at,id",
        id);
  }
}
