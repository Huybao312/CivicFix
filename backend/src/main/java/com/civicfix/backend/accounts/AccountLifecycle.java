package com.civicfix.backend.accounts;

import static com.civicfix.backend.accounts.ApiErrors.Problem;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountLifecycle {
  final JdbcTemplate db;
  final AccountAccess access;
  final LocalMailService mail;
  final PasswordEncoder passwords;
  final PasswordPolicy policy;
  final Clock clock;
  final RequestLimits limits;
  final RecoveryDelivery recovery;
  private final String origin;
  private final SecureRandom random = new SecureRandom();

  public AccountLifecycle(
      JdbcTemplate db,
      AccountAccess access,
      LocalMailService mail,
      PasswordEncoder passwords,
      PasswordPolicy policy,
      Clock clock,
      RequestLimits limits,
      RecoveryDelivery recovery,
      @Value("${civicfix.public-origin:http://localhost:5173}") String origin) {
    this.db = db;
    this.access = access;
    this.mail = mail;
    this.passwords = passwords;
    this.policy = policy;
    this.clock = clock;
    this.limits = limits;
    this.recovery = recovery;
    if (!origin.matches("https?://(localhost|127\\.0\\.0\\.1)(:[0-9]+)?"))
      throw new IllegalStateException("Demo links must use a local origin");
    this.origin = origin;
  }

  public static String normalize(String email) {
    return email.strip().toLowerCase(Locale.ROOT);
  }

  static String hash(String raw) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8)));
    } catch (java.security.NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private String token() {
    byte[] bytes = new byte[32];
    random.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  Timestamp now() {
    return Timestamp.from(clock.instant());
  }

  static UUID uuid(Map<String, Object> row, String key) {
    return (UUID) row.get(key);
  }

  static Instant instant(Map<String, Object> row, String key) {
    return ((Timestamp) row.get(key)).toInstant();
  }

  private Map<String, Object> one(String sql, Object... args) {
    var rows = db.queryForList(sql, args);
    if (rows.isEmpty()) throw new Problem(404, "Không tìm thấy dữ liệu trong phạm vi của bạn.");
    return rows.getFirst();
  }

  private void lock(UUID building, String email) {
    db.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))", building + ":" + email);
  }

  private void lockUser(UUID user) {
    db.queryForList("SELECT id FROM users WHERE id=? FOR UPDATE", user);
  }

  public void publicLimit(String ip) {
    if (!limits.take("public:" + ip, 90, 60))
      throw new Problem(429, "Bạn đã thử nhiều lần. Vui lòng thử lại sau.");
  }

  private Map<String, Object> invitation(String token, boolean lock) {
    if (token == null || token.length() > 100)
      throw new Problem(410, "Liên kết không còn dùng được.");
    var rows =
        db.queryForList(
            "SELECT * FROM account_invitations WHERE token_hash=?" + (lock ? " FOR UPDATE" : ""),
            hash(token));
    if (rows.isEmpty()) throw new Problem(410, "Liên kết không còn dùng được.");
    var i = rows.getFirst();
    if (!"PENDING".equals(i.get("status")) || !instant(i, "expires_at").isAfter(clock.instant()))
      throw new Problem(
          410, "Liên kết không còn dùng được. Hãy liên hệ quản trị viên để nhận lời mời mới.");
    return i;
  }

  public Map<String, Object> preview(String token) {
    var i = invitation(token, false);
    var user = one("SELECT status FROM users WHERE id=?", i.get("user_id"));
    if ("DISABLED".equals(user.get("status")))
      throw new Problem(410, "Liên kết không còn dùng được. Hãy liên hệ quản trị viên.");
    var view =
        new LinkedHashMap<>(
            one(
                """
                SELECT b.name AS building_name,a.unit_number,u.email,u.display_name
                FROM users u,buildings b,apartments a WHERE u.id=? AND b.id=? AND a.id=?
                """,
                i.get("user_id"),
                i.get("building_id"),
                i.get("apartment_id")));
    view.put("existing_account", "ACTIVE".equals(user.get("status")));
    view.put("expires_at", instant(i, "expires_at").toString());
    return view;
  }

  private void closePendingMembership(UUID id) {
    db.update("UPDATE building_memberships SET status='ENDED' WHERE id=? AND status='PENDING'", id);
  }

  private void invitationMail(Map<String, Object> i, String rawToken) {
    var info =
        one(
            """
            SELECT u.email,b.name,a.unit_number,u.status FROM users u,buildings b,apartments a
            WHERE u.id=? AND b.id=? AND a.id=?
            """,
            i.get("user_id"),
            i.get("building_id"),
            i.get("apartment_id"));
    mail.send(
        (String) info.get("email"),
        "Lời mời tham gia CivicFix – " + info.get("name"),
        "Bạn được mời vào "
            + info.get("name")
            + ", căn "
            + info.get("unit_number")
            + ".\n"
            + ("ACTIVE".equals(info.get("status"))
                ? "Đăng nhập bằng mật khẩu hiện tại để chấp nhận.\n"
                : "Bạn sẽ tự đặt mật khẩu khi kích hoạt.\n")
            + "Liên kết có hạn 24 giờ và chỉ dùng một lần:\n"
            + origin
            + "/activate#token="
            + rawToken
            + "\n"
            + "Nếu bạn không mong đợi lời mời này, không mở liên kết; liên hệ đầu mối quản trị đã"
            + " công bố.");
  }

  @Transactional
  public Map<String, Object> invite(UUID actor, UUID building, AdminController.Invite r) {
    access.admin(actor, building);
    String email = normalize(r.email());
    if (!email.endsWith("@demo.civicfix.local"))
      throw new Problem(422, "Chỉ dùng email giả @demo.civicfix.local trong demo.");
    if (!Set.of("RESIDENT_LIST", "OPERATOR_CONFIRMATION").contains(r.verificationSource()))
      throw new Problem(422, "Chọn căn cứ xác minh hợp lệ.");
    lock(building, email);
    one("SELECT id FROM apartments WHERE id=? AND building_id=?", r.apartmentId(), building);
    var users = db.queryForList("SELECT id,status FROM users WHERE email=?", email);
    UUID user;
    if (users.isEmpty()) {
      user = UUID.randomUUID();
      // Cross-building invitations can race on the same global email identity.
      db.update(
          "INSERT INTO users(id,email,display_name) VALUES (?,?,?) ON CONFLICT(email) DO NOTHING",
          user,
          email,
          r.displayName().strip());
      user = (UUID) db.queryForMap("SELECT id FROM users WHERE email=?", email).get("id");
    } else user = uuid(users.getFirst(), "id");
    lockUser(user);
    if ("DISABLED".equals(one("SELECT status FROM users WHERE id=?", user).get("status")))
      throw new Problem(409, "Tài khoản đang bị vô hiệu hóa. Không thể gửi lời mời kích hoạt lại.");
    var old =
        db.queryForList(
            "SELECT * FROM account_invitations WHERE user_id=? AND building_id=? AND"
                + " status='PENDING' FOR UPDATE",
            user,
            building);
    if (!old.isEmpty()) {
      var i = old.getFirst();
      if (instant(i, "expires_at").isAfter(clock.instant())) {
        if (!r.apartmentId().equals(i.get("apartment_id")))
          throw new Problem(409, "Đã có lời mời khác đang chờ. Hãy thu hồi lời mời cũ trước.");
        return Map.of("id", i.get("id"), "already_pending", true);
      }
      db.update("UPDATE account_invitations SET status='EXPIRED' WHERE id=?", i.get("id"));
      closePendingMembership(uuid(i, "membership_id"));
    }
    if (Boolean.TRUE.equals(
        db.queryForObject(
            "SELECT EXISTS(SELECT 1 FROM resident_apartment_links WHERE user_id=? AND"
                + " apartment_id=? AND status='VERIFIED')",
            Boolean.class,
            user,
            r.apartmentId())))
      throw new Problem(
          409, "Căn hộ đã được liên kết với cư dân này. Kiểm tra liên kết hiện có trước.");
    var memberships =
        db.queryForList(
            "SELECT id,status FROM building_memberships WHERE user_id=? AND building_id=? AND"
                + " role='RESIDENT' AND status IN ('PENDING','ACTIVE')",
            user,
            building);
    UUID membership;
    if (memberships.isEmpty()) {
      membership = UUID.randomUUID();
      db.update(
          "INSERT INTO building_memberships(id,user_id,building_id,role) VALUES (?,?,?,'RESIDENT')",
          membership,
          user,
          building);
    } else membership = uuid(memberships.getFirst(), "id");
    UUID id = UUID.randomUUID();
    String raw = token();
    var start = r.validFrom() == null ? clock.instant() : r.validFrom().toInstant();
    db.update(
        """
        INSERT INTO account_invitations(id,user_id,building_id,apartment_id,membership_id,invited_by,
            verification_source,valid_from,token_hash,expires_at)
        VALUES (?,?,?,?,?,?,?,?,?,?)
        """,
        id,
        user,
        building,
        r.apartmentId(),
        membership,
        actor,
        r.verificationSource(),
        Timestamp.from(start),
        hash(raw),
        Timestamp.from(clock.instant().plus(Duration.ofHours(24))));
    invitationMail(one("SELECT * FROM account_invitations WHERE id=?", id), raw);
    access.audit(actor, building, "INVITATION_CREATED", id, r.verificationSource());
    return Map.of("id", id, "already_pending", false);
  }

  @Transactional
  public void resend(UUID actor, UUID building, UUID id) {
    access.admin(actor, building);
    var preliminary =
        one("SELECT user_id FROM account_invitations WHERE id=? AND building_id=?", id, building);
    lockUser(uuid(preliminary, "user_id"));
    var i =
        one(
            "SELECT * FROM account_invitations WHERE id=? AND building_id=? FOR UPDATE",
            id,
            building);
    if (!"PENDING".equals(i.get("status"))) throw new Problem(409, "Chỉ gửi lại lời mời đang chờ.");
    if (instant(i, "last_sent_at").plusSeconds(60).isAfter(clock.instant()))
      throw new Problem(429, "Chờ ít nhất 60 giây trước khi gửi lại.");
    lockUser(uuid(i, "user_id"));
    if ("DISABLED"
        .equals(one("SELECT status FROM users WHERE id=?", i.get("user_id")).get("status")))
      throw new Problem(409, "Tài khoản đang bị vô hiệu hóa.");
    String raw = token();
    db.update(
        "UPDATE account_invitations SET token_hash=?,expires_at=?,last_sent_at=? WHERE id=?",
        hash(raw),
        Timestamp.from(clock.instant().plus(Duration.ofHours(24))),
        now(),
        id);
    invitationMail(i, raw);
    access.audit(actor, building, "INVITATION_RESENT", id, null);
  }

  @Transactional
  public void revoke(UUID actor, UUID building, UUID id, String reason) {
    access.admin(actor, building);
    var preliminary =
        one("SELECT user_id FROM account_invitations WHERE id=? AND building_id=?", id, building);
    lockUser(uuid(preliminary, "user_id"));
    var i =
        one(
            "SELECT * FROM account_invitations WHERE id=? AND building_id=? FOR UPDATE",
            id,
            building);
    if (!"PENDING".equals(i.get("status")))
      throw new Problem(409, "Lời mời không còn ở trạng thái chờ.");
    db.update(
        "UPDATE account_invitations SET status='REVOKED',revoked_at=?,reason=? WHERE id=?",
        now(),
        reason,
        id);
    closePendingMembership(uuid(i, "membership_id"));
    access.audit(actor, building, "INVITATION_REVOKED", id, reason);
  }

  @Transactional
  public void incorrect(String token) {
    var preliminary = invitation(token, false);
    lockUser(uuid(preliminary, "user_id"));
    var i = invitation(token, true);
    db.update(
        "UPDATE account_invitations SET"
            + " status='REVOKED',revoked_at=?,incorrect_details=TRUE,reason=? WHERE id=?",
        now(),
        "Người nhận báo thông tin căn hộ không đúng",
        i.get("id"));
    closePendingMembership(uuid(i, "membership_id"));
    access.audit(
        null,
        uuid(i, "building_id"),
        "INVITATION_DETAILS_REPORTED",
        uuid(i, "id"),
        "Cần quản trị viên kiểm tra lại căn hộ");
  }

  @Transactional
  public void accept(String token, String password, UUID actor) {
    var preliminary = invitation(token, false);
    UUID user = uuid(preliminary, "user_id");
    lockUser(user);
    var i = invitation(token, true);
    var u = one("SELECT status FROM users WHERE id=?", user);
    if ("DISABLED".equals(u.get("status")))
      throw new Problem(410, "Liên kết không còn dùng được. Hãy liên hệ quản trị viên.");
    var m =
        one(
            "SELECT status FROM building_memberships WHERE id=? FOR UPDATE",
            i.get("membership_id"));
    if ("ENDED".equals(m.get("status"))) throw new Problem(410, "Quyền trong lời mời đã kết thúc.");
    if ("INVITED".equals(u.get("status"))) {
      policy.check(password);
      db.update(
          "UPDATE users SET"
              + " password_hash=?,email_verified_at=?,status='ACTIVE',auth_version=auth_version+1"
              + " WHERE id=?",
          passwords.encode(password),
          now(),
          user);
    } else if (!user.equals(actor))
      throw new Problem(403, "Đăng nhập bằng đúng tài khoản nhận lời mời để chấp nhận.");
    if ("PENDING".equals(m.get("status")))
      db.update(
          "UPDATE building_memberships SET status='ACTIVE',valid_from=?,valid_to=NULL WHERE id=?",
          i.get("valid_from"),
          i.get("membership_id"));
    db.update(
        """
        INSERT INTO resident_apartment_links(id,user_id,apartment_id,verified_by,verification_source,valid_from)
        VALUES (?,?,?,?,?,?) ON CONFLICT(user_id,apartment_id) WHERE status='VERIFIED' DO NOTHING
        """,
        UUID.randomUUID(),
        user,
        i.get("apartment_id"),
        i.get("invited_by"),
        i.get("verification_source"),
        i.get("valid_from"));
    db.update(
        "UPDATE account_invitations SET status='ACCEPTED',used_at=? WHERE id=?",
        now(),
        i.get("id"));
    access.audit(user, uuid(i, "building_id"), "INVITATION_ACCEPTED", uuid(i, "id"), null);
  }

  @Transactional
  public void forgot(String email, String ip) {
    email = normalize(email);
    if (!limits.take("forgot-ip:" + ip, 30, 3600)
        || !limits.take("forgot-email:" + hash(email), 3, 3600)) return;
    var rows =
        db.queryForList(
            "SELECT id,email FROM users WHERE email=? AND status='ACTIVE' AND email_verified_at IS"
                + " NOT NULL FOR UPDATE",
            email);
    if (rows.isEmpty()) return;
    UUID user = uuid(rows.getFirst(), "id");
    lockUser(user);
    db.update(
        "UPDATE password_reset_tokens SET used_at=? WHERE user_id=? AND used_at IS NULL",
        now(),
        user);
    String raw = token();
    db.update(
        "INSERT INTO password_reset_tokens(id,user_id,token_hash,expires_at) VALUES (?,?,?,?)",
        UUID.randomUUID(),
        user,
        hash(raw),
        Timestamp.from(clock.instant().plusSeconds(1800)));
    recovery.afterCommit(
        user,
        email,
        "Liên kết chỉ dùng một lần và hết hạn sau 30 phút:\n"
            + origin
            + "/reset-password#token="
            + raw
            + "\nNếu không yêu cầu, bạn có thể bỏ qua email này.");
    access.audit(user, null, "PASSWORD_RESET_REQUESTED", user, null);
  }

  @Transactional
  public void reset(String token, String password) {
    policy.check(password);
    var initial =
        db.queryForList(
            "SELECT user_id FROM password_reset_tokens WHERE token_hash=?", hash(token));
    if (initial.isEmpty()) throw new Problem(410, "Liên kết không còn dùng được.");
    lockUser(uuid(initial.getFirst(), "user_id"));
    var rows =
        db.queryForList(
            "SELECT * FROM password_reset_tokens WHERE token_hash=? FOR UPDATE", hash(token));
    if (rows.isEmpty()) throw new Problem(410, "Liên kết không còn dùng được.");
    var r = rows.getFirst();
    UUID user = uuid(r, "user_id");
    lockUser(user);
    var u = one("SELECT status,email FROM users WHERE id=?", user);
    if (r.get("used_at") != null
        || !instant(r, "expires_at").isAfter(clock.instant())
        || !"ACTIVE".equals(u.get("status")))
      throw new Problem(410, "Liên kết không còn dùng được.");
    db.update(
        "UPDATE users SET password_hash=?,auth_version=auth_version+1 WHERE id=?",
        passwords.encode(password),
        user);
    db.update(
        "UPDATE password_reset_tokens SET used_at=? WHERE user_id=? AND used_at IS NULL",
        now(),
        user);
    mail.send(
        (String) u.get("email"),
        "Mật khẩu CivicFix đã được đổi",
        "Mật khẩu đã được đổi và các phiên đăng nhập cũ đã được thu hồi. Nếu không phải bạn thực"
            + " hiện, hãy liên hệ quản trị viên qua kênh đã công bố.");
    access.audit(user, null, "PASSWORD_CHANGED", user, null);
  }
}
