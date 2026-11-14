package com.civicfix.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.civicfix.backend.accounts.RequestLimits;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class TriageTests {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Autowired PasswordEncoder encoder;
  @Autowired RequestLimits limits;
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String PASSWORD = "A private triage test passphrase 2026";
  private static String passwordHash;
  private UUID building,
      otherBuilding,
      resident,
      stranger,
      manager,
      secondManager,
      technician,
      otherTechnician,
      foreignTechnician,
      admin,
      issue;
  private MockHttpSession residentSession, managerSession, technicianSession;

  @BeforeEach
  void prepare() throws Exception {
    limits.clear();
    if (passwordHash == null) passwordHash = encoder.encode(PASSWORD);
    building = building();
    otherBuilding = building();
    resident = user("Người báo thử nghiệm");
    stranger = user("Cư dân khác");
    manager = user("Quản lý một");
    secondManager = user("Quản lý hai");
    technician = user("Kỹ thuật viên được giao");
    otherTechnician = user("Kỹ thuật viên khác");
    foreignTechnician = user("Kỹ thuật viên chung cư khác");
    admin = user("Quản trị viên");
    member(resident, building, "RESIDENT");
    member(stranger, building, "RESIDENT");
    member(manager, building, "MANAGER");
    member(secondManager, building, "MANAGER");
    member(technician, building, "TECHNICIAN");
    member(otherTechnician, building, "TECHNICIAN");
    member(foreignTechnician, otherBuilding, "TECHNICIAN");
    member(admin, building, "ADMIN");
    UUID apartment = UUID.randomUUID();
    db.update(
        "INSERT INTO apartments(id,building_id,unit_number) VALUES (?,?,'03')",
        apartment,
        building);
    db.update(
        "INSERT INTO"
            + " resident_apartment_links(id,user_id,apartment_id,verified_by,verification_source)"
            + " VALUES (?,?,?,?,'RESIDENT_LIST')",
        UUID.randomUUID(),
        resident,
        apartment,
        admin);
    residentSession = login(resident);
    managerSession = login(manager);
    technicianSession = login(technician);
    issue = createReport();
  }

  private UUID createReport() throws Exception {
    var response =
        mvc.perform(
                post(base())
                    .session(residentSession)
                    .with(csrf())
                    .header("Idempotency-Key", UUID.randomUUID())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        JSON.writeValueAsString(
                            Map.of(
                                "scope",
                                "COMMON_AREA",
                                "locationDescription",
                                "Hành lang tầng 3",
                                "description",
                                "Đèn chớp liên tục vào buổi tối."))))
            .andExpect(status().isOk())
            .andReturn();
    return UUID.fromString(
        object(response.getResponse().getContentAsString()).get("id").toString());
  }

  private UUID building() {
    UUID id = UUID.randomUUID();
    db.update(
        "INSERT INTO buildings(id,name,address,time_zone) VALUES (?,'Chung cư test','Địa chỉ"
            + " giả','Asia/Ho_Chi_Minh')",
        id);
    return id;
  }

  private UUID user(String name) {
    UUID id = UUID.randomUUID();
    db.update(
        "INSERT INTO users(id,email,display_name,password_hash,status,email_verified_at) VALUES"
            + " (?,?,?,?,'ACTIVE',CURRENT_TIMESTAMP)",
        id,
        id + "@demo.civicfix.local",
        name,
        passwordHash);
    return id;
  }

  private void member(UUID user, UUID scope, String role) {
    db.update(
        "INSERT INTO building_memberships(id,user_id,building_id,role,status) VALUES"
            + " (?,?,?,?,'ACTIVE')",
        UUID.randomUUID(),
        user,
        scope,
        role);
  }

  private MockHttpSession login(UUID user) throws Exception {
    return (MockHttpSession)
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .param("email", user + "@demo.civicfix.local")
                    .param("password", PASSWORD))
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession(false);
  }

  private String base() {
    return "/api/buildings/" + building + "/issues";
  }

  private String notifications() {
    return "/api/buildings/" + building + "/notifications";
  }

  private Map<String, Object> body(long version, String text) {
    return new HashMap<>(Map.of("expectedVersion", version, "text", text));
  }

  private ResultActions command(
      MockHttpSession session, String action, UUID key, Map<String, Object> body) throws Exception {
    return mvc.perform(
        post(base() + "/" + issue + "/actions/" + action)
            .session(session)
            .with(csrf())
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(JSON.writeValueAsString(body)));
  }

  private Map<String, Object> act(MockHttpSession session, String action, long version, String text)
      throws Exception {
    return object(
        command(session, action, UUID.randomUUID(), body(version, text))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString());
  }

  private void accept() throws Exception {
    act(managerSession, "ACCEPT", 0, "Đã kiểm tra đúng phạm vi và đủ dữ liệu.");
  }

  private void assign(UUID target, long version) throws Exception {
    var body = body(version, "Giao kiểm tra đèn hành lang.");
    body.put("technicianId", target);
    command(managerSession, "ASSIGN", UUID.randomUUID(), body).andExpect(status().isOk());
  }

  private Map<String, Object> object(String json) throws Exception {
    return JSON.readValue(json, Map.class);
  }

  private Map<String, Object> persisted() {
    return db.queryForMap("SELECT * FROM issues WHERE id=?", issue);
  }

  private int count(String table) {
    return db.queryForObject(
        "SELECT count(*) FROM " + table + " WHERE issue_id=?", Integer.class, issue);
  }

  private String history(MockHttpSession session) throws Exception {
    return mvc.perform(get(base() + "/" + issue + "/events").session(session))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse()
        .getContentAsString();
  }

  private List<Map<String, Object>> notices(MockHttpSession session) throws Exception {
    return JSON.readValue(
        mvc.perform(get(notifications()).session(session))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(),
        List.class);
  }

  @Test
  void acceptancePriorityAndAssignmentProduceOneCurrentOwner() throws Exception {
    var accepted = act(managerSession, "ACCEPT", 0, "Đã kiểm tra đủ dữ liệu.");
    assertEquals("ACCEPTED", accepted.get("status"));
    assertEquals(1, ((Number) accepted.get("version")).intValue());
    var priority = body(1, "Hành lang thiếu sáng cần kiểm tra sớm.");
    priority.put("priority", "HIGH");
    command(managerSession, "SET_PRIORITY", UUID.randomUUID(), priority)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.priority").value("HIGH"));
    assign(technician, 2);
    assertEquals("ASSIGNED", persisted().get("status"));
    assertEquals(3L, persisted().get("version"));
    assertEquals(1, count("issue_assignments"));
    assertEquals(4, count("issue_events"));
    assertEquals(
        technician,
        db.queryForObject(
            "SELECT technician_id FROM issue_assignments WHERE issue_id=? AND ended_at IS NULL",
            UUID.class,
            issue));
    mvc.perform(get(base() + "/" + issue).session(managerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reporter_name_at_report").value("Người báo thử nghiệm"))
        .andExpect(jsonPath("$.apartment_label_at_report").value("03"));
  }

  @Test
  void questionAndReplyPreserveOriginalDescriptionAndNotifyReporter() throws Exception {
    String original = persisted().get("description").toString();
    var asked = act(managerSession, "REQUEST_INFO", 0, "Đèn ở gần thang máy nào?");
    assertEquals("NEEDS_INFO", asked.get("status"));
    assertEquals("Đèn ở gần thang máy nào?", asked.get("question"));
    var notifications = notices(residentSession);
    assertEquals(1, notifications.size());
    assertNull(notifications.getFirst().get("read_at"));
    var answered = act(residentSession, "PROVIDE_INFO", 1, "Đèn ở gần thang máy B, tầng 3.");
    assertEquals("SUBMITTED", answered.get("status"));
    assertEquals(original, persisted().get("description"));
    assertEquals(3, count("issue_events"));
    String history = history(residentSession);
    assertTrue(history.contains("Đèn ở gần thang máy nào?"));
    assertTrue(history.contains("Đèn ở gần thang máy B, tầng 3."));
  }

  @Test
  void rejectionAndRestorationKeepBothPublicReasons() throws Exception {
    act(managerSession, "REJECT", 0, "Thiết bị thuộc phạm vi riêng cần kiểm tra lại.");
    assertEquals("REJECTED", persisted().get("status"));
    act(managerSession, "RESTORE", 1, "Đối chiếu lại xác nhận đây là đèn hành lang chung.");
    assertEquals("SUBMITTED", persisted().get("status"));
    assertEquals(3, count("issue_events"));
    String history = history(residentSession);
    assertTrue(history.contains("Thiết bị thuộc phạm vi riêng"));
    assertTrue(history.contains("Đối chiếu lại xác nhận"));
  }

  @Test
  void invalidTransitionsHaveNoEffects() throws Exception {
    command(
            managerSession,
            "ASSIGN",
            UUID.randomUUID(),
            new HashMap<>(
                Map.of("expectedVersion", 0, "text", "Giao việc", "technicianId", technician)))
        .andExpect(status().isConflict());
    command(managerSession, "RESTORE", UUID.randomUUID(), body(0, "Thử khôi phục sai trạng thái"))
        .andExpect(status().isConflict());
    command(
            residentSession,
            "PROVIDE_INFO",
            UUID.randomUUID(),
            body(0, "Trả lời khi chưa được hỏi"))
        .andExpect(status().isConflict());
    assertEquals(0L, persisted().get("version"));
    assertEquals(1, count("issue_events"));
    assertEquals(0, count("issue_commands"));
    accept();
    command(managerSession, "ACCEPT", UUID.randomUUID(), body(1, "Tiếp nhận lần hai"))
        .andExpect(status().isConflict());
    command(managerSession, "REQUEST_INFO", UUID.randomUUID(), body(1, "Hỏi ở trạng thái sai"))
        .andExpect(status().isConflict());
    assertEquals(1L, persisted().get("version"));
    assertEquals(2, count("issue_events"));
  }

  @Test
  void requiredReasonsAndQuestionsCannotBeBlank() throws Exception {
    for (String action : List.of("ACCEPT", "REJECT", "REQUEST_INFO", "SET_PRIORITY")) {
      var body = body(0, "   ");
      if (action.equals("SET_PRIORITY")) body.put("priority", "HIGH");
      command(managerSession, action, UUID.randomUUID(), body)
          .andExpect(status().isUnprocessableContent());
    }
    assertEquals(0L, persisted().get("version"));
    assertEquals(1, count("issue_events"));
    act(managerSession, "REJECT", 0, "Cần xác minh lại phạm vi báo cáo.");
    command(managerSession, "RESTORE", UUID.randomUUID(), body(1, " "))
        .andExpect(status().isUnprocessableContent());
    assertEquals("REJECTED", persisted().get("status"));
  }

  @Test
  void priorityRejectsUnknownValueAndRecordsVersion() throws Exception {
    var bad = body(0, "Một mức ưu tiên không được định nghĩa.");
    bad.put("priority", "CRITICAL");
    command(managerSession, "SET_PRIORITY", UUID.randomUUID(), bad)
        .andExpect(status().isUnprocessableContent());
    for (String value : List.of("LOW", "MEDIUM", "HIGH", "URGENT")) {
      long version = ((Number) persisted().get("version")).longValue();
      var body = body(version, "Xem xét mức ưu tiên " + value);
      body.put("priority", value);
      command(managerSession, "SET_PRIORITY", UUID.randomUUID(), body)
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.priority").value(value));
    }
    assertEquals(4L, persisted().get("version"));
    assertEquals(5, count("issue_events"));
  }

  @Test
  void sameKeyRetryDoesNotDuplicateEventsOrNotifications() throws Exception {
    UUID key = UUID.randomUUID();
    var body = body(0, "Đã kiểm tra đủ phạm vi.");
    var first =
        command(managerSession, "ACCEPT", key, body)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    var second =
        command(managerSession, "ACCEPT", key, body)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertEquals(first, second);
    assertEquals(1L, persisted().get("version"));
    assertEquals(2, count("issue_events"));
    assertEquals(1, count("issue_commands"));
    assertEquals(1, notices(residentSession).size());
  }

  @Test
  void sameKeyWithChangedBodyOrActionConflicts() throws Exception {
    UUID key = UUID.randomUUID();
    command(managerSession, "ACCEPT", key, body(0, "Đã xác nhận phạm vi."))
        .andExpect(status().isOk());
    command(managerSession, "ACCEPT", key, body(0, "Nội dung khác với request đầu."))
        .andExpect(status().isConflict());
    command(managerSession, "REJECT", key, body(0, "Đã xác nhận phạm vi."))
        .andExpect(status().isConflict());
    assertEquals(1L, persisted().get("version"));
    assertEquals(1, count("issue_commands"));
    assertEquals(2, count("issue_events"));
  }

  @Test
  void staleVersionReturnsCurrentStateWithoutOverwritingIt() throws Exception {
    accept();
    command(
            login(secondManager),
            "REJECT",
            UUID.randomUUID(),
            body(0, "Quyết định dựa trên dữ liệu cũ."))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.currentVersion").value(1))
        .andExpect(jsonPath("$.currentStatus").value("ACCEPTED"));
    assertEquals("ACCEPTED", persisted().get("status"));
    assertEquals(2, count("issue_events"));
    assertEquals(1, count("issue_commands"));
  }

  @Test
  void twoManagersAssigningDifferentTechniciansAtSameVersionHaveOneWinner() throws Exception {
    accept();
    MockHttpSession second = login(secondManager);
    var firstBody = body(1, "Giao kỹ thuật viên thứ nhất kiểm tra.");
    firstBody.put("technicianId", technician);
    var secondBody = body(1, "Giao kỹ thuật viên thứ hai kiểm tra.");
    secondBody.put("technicianId", otherTechnician);
    var start = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(2);
    try {
      var first =
          pool.submit(
              () -> {
                start.await();
                return command(managerSession, "ASSIGN", UUID.randomUUID(), firstBody)
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      var other =
          pool.submit(
              () -> {
                start.await();
                return command(second, "ASSIGN", UUID.randomUUID(), secondBody)
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      start.countDown();
      var statuses =
          new ArrayList<>(
              List.of(first.get(10, TimeUnit.SECONDS), other.get(10, TimeUnit.SECONDS)));
      Collections.sort(statuses);
      assertEquals(List.of(200, 409), statuses);
      assertEquals(2L, persisted().get("version"));
      assertEquals("ASSIGNED", persisted().get("status"));
      assertEquals(3, count("issue_events"));
      assertEquals(2, count("issue_commands"));
      assertEquals(1, count("issue_assignments"));
      assertEquals(2, notices(residentSession).size());
      UUID winner =
          db.queryForObject(
              "SELECT technician_id FROM issue_assignments WHERE issue_id=? AND ended_at IS NULL",
              UUID.class,
              issue);
      UUID loser = winner.equals(technician) ? otherTechnician : technician;
      assertEquals(
          1,
          db.queryForObject(
              "SELECT count(*) FROM issue_notifications WHERE issue_id=? AND recipient_id=?",
              Integer.class,
              issue,
              winner));
      assertEquals(
          0,
          db.queryForObject(
              "SELECT count(*) FROM issue_notifications WHERE issue_id=? AND recipient_id=?",
              Integer.class,
              issue,
              loser));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void assigningInvalidTechnicianLeavesStateAndAssignmentsUntouched() throws Exception {
    UUID disabled = user("Bị khóa"), expired = user("Hết quyền"), future = user("Chưa có quyền");
    for (UUID user : List.of(disabled, expired, future)) member(user, building, "TECHNICIAN");
    db.update("UPDATE users SET status='DISABLED' WHERE id=?", disabled);
    db.update(
        "UPDATE building_memberships SET valid_from=CURRENT_TIMESTAMP-INTERVAL '2"
            + " days',valid_to=CURRENT_TIMESTAMP-INTERVAL '1 day' WHERE user_id=?",
        expired);
    db.update(
        "UPDATE building_memberships SET valid_from=CURRENT_TIMESTAMP+INTERVAL '1 day' WHERE"
            + " user_id=?",
        future);
    accept();
    for (UUID target : List.of(manager, foreignTechnician, disabled, expired, future)) {
      var body = body(1, "Giao cho người không đủ điều kiện.");
      body.put("technicianId", target);
      command(managerSession, "ASSIGN", UUID.randomUUID(), body)
          .andExpect(status().is4xxClientError());
    }
    assertEquals("ACCEPTED", persisted().get("status"));
    assertEquals(1L, persisted().get("version"));
    assertEquals(0, count("issue_assignments"));
    assertEquals(2, count("issue_events"));
  }

  @Test
  void unauthorizedRolesAndOtherBuildingsCannotTriage() throws Exception {
    UUID foreignManager = user("Quản lý chung cư khác");
    member(foreignManager, otherBuilding, "MANAGER");
    for (var session :
        List.of(residentSession, technicianSession, login(admin), login(foreignManager)))
      command(session, "ACCEPT", UUID.randomUUID(), body(0, "Thử vượt quyền."))
          .andExpect(status().is4xxClientError());
    assertEquals(0L, persisted().get("version"));
    assertEquals(1, count("issue_events"));
    assertEquals(0, count("issue_commands"));
  }

  @Test
  void onlyLinkedReporterCanProvideWebInformation() throws Exception {
    act(managerSession, "REQUEST_INFO", 0, "Xin xác nhận vị trí đèn.");
    for (var session : List.of(login(stranger), managerSession, technicianSession))
      command(session, "PROVIDE_INFO", UUID.randomUUID(), body(1, "Cố trả lời thay cư dân."))
          .andExpect(status().is4xxClientError());
    assertEquals("NEEDS_INFO", persisted().get("status"));
    assertEquals(2, count("issue_events"));
    act(residentSession, "PROVIDE_INFO", 1, "Đèn nằm cạnh thang máy B.");
    assertEquals("SUBMITTED", persisted().get("status"));
  }

  @Test
  void onlyAssignedTechnicianCanReadListDetailAndHistoryWithoutReporterFields() throws Exception {
    accept();
    assign(technician, 1);
    var detail =
        object(
            mvc.perform(get(base() + "/" + issue).session(technicianSession))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    for (String secret :
        List.of(
            "reported_by",
            "reporter_name_at_report",
            "apartment_id_at_report",
            "apartment_label_at_report")) assertFalse(detail.containsKey(secret), secret);
    assertEquals(issue.toString(), detail.get("id"));
    assertFalse(history(technicianSession).contains("Người báo thử nghiệm"));
    String list =
        mvc.perform(get(base()).session(technicianSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1))
            .andReturn()
            .getResponse()
            .getContentAsString();
    for (String secret :
        List.of(
            "reported_by",
            "reporter_name_at_report",
            "apartment_id_at_report",
            "apartment_label_at_report")) assertFalse(list.contains(secret), secret);
    var other = login(otherTechnician);
    mvc.perform(get(base()).session(other))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));
    mvc.perform(get(base() + "/" + issue).session(other)).andExpect(status().isNotFound());
    mvc.perform(get(base() + "/" + issue + "/events").session(other))
        .andExpect(status().isNotFound());
  }

  @Test
  void endedAssignmentImmediatelyStopsOldTechnicianAccess() throws Exception {
    accept();
    assign(technician, 1);
    db.update("UPDATE issue_assignments SET ended_at=CURRENT_TIMESTAMP WHERE issue_id=?", issue);
    mvc.perform(get(base() + "/" + issue).session(technicianSession))
        .andExpect(status().isNotFound());
    mvc.perform(get(base() + "/" + issue + "/events").session(technicianSession))
        .andExpect(status().isNotFound());
    mvc.perform(get(base()).session(technicianSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));
  }

  @Test
  void endedMembershipAndDisabledAccountStopTechnicianAccess() throws Exception {
    accept();
    assign(technician, 1);
    db.update(
        "UPDATE building_memberships SET status='ENDED' WHERE user_id=? AND role='TECHNICIAN'",
        technician);
    mvc.perform(get(base() + "/" + issue).session(technicianSession))
        .andExpect(status().isNotFound());
    db.update("UPDATE users SET status='DISABLED' WHERE id=?", technician);
    mvc.perform(get(base() + "/" + issue).session(technicianSession))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void multiRoleTechnicianManagerReceivesManagerProjection() throws Exception {
    member(technician, building, "MANAGER");
    mvc.perform(get(base() + "/" + issue).session(technicianSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reported_by").value(resident.toString()))
        .andExpect(jsonPath("$.apartment_label_at_report").value("03"));
  }

  @Test
  void residentTechnicianOnlySeesAssignmentDetailsForTheirOwnAssignedWork() throws Exception {
    member(resident, building, "TECHNICIAN");
    accept();
    assign(otherTechnician, 1);
    var detail =
        object(
            mvc.perform(get(base() + "/" + issue).session(residentSession))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertEquals(resident.toString(), detail.get("reported_by"));
    assertNull(detail.get("assignment"));
    assertEquals(false, ((Map<?, ?>) detail.get("permissions")).get("manage"));

    issue = createReport();
    accept();
    assign(resident, 1);
    var ownWork =
        object(
            mvc.perform(get(base() + "/" + issue).session(residentSession))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    assertEquals(resident.toString(), ((Map<?, ?>) ownWork.get("assignment")).get("technician_id"));
  }

  @Test
  void managerMembershipExpiringWhileWaitingForIssueLockDeniesCommand() throws Exception {
    assertEquals(
        403,
        commandAfterMembershipExpiresWhileBlocked(
            manager, "MANAGER", "ACCEPT", body(0, "Quyết định phải còn quyền khi lấy được khóa.")));
    assertEquals("SUBMITTED", persisted().get("status"));
    assertEquals(0L, persisted().get("version"));
    assertEquals(1, count("issue_events"));
    assertEquals(0, count("issue_commands"));
    assertEquals(0, count("issue_notifications"));
  }

  @Test
  void technicianMembershipExpiringWhileWaitingForIssueLockDeniesAssignment() throws Exception {
    accept();
    var assignment = body(1, "Kỹ thuật viên phải còn quyền khi phân công commit.");
    assignment.put("technicianId", technician);
    assertEquals(
        422,
        commandAfterMembershipExpiresWhileBlocked(technician, "TECHNICIAN", "ASSIGN", assignment));
    assertEquals("ACCEPTED", persisted().get("status"));
    assertEquals(1L, persisted().get("version"));
    assertEquals(2, count("issue_events"));
    assertEquals(1, count("issue_commands"));
    assertEquals(0, count("issue_assignments"));
    assertEquals(1, count("issue_notifications"));
  }

  private int commandAfterMembershipExpiresWhileBlocked(
      UUID expiringUser, String role, String action, Map<String, Object> payload) throws Exception {
    var pool = Executors.newSingleThreadExecutor();
    try (var holder = Objects.requireNonNull(db.getDataSource()).getConnection()) {
      holder.setAutoCommit(false);
      int holderPid;
      try (var pid = holder.prepareStatement("SELECT pg_backend_pid()");
          var row = pid.executeQuery()) {
        assertTrue(row.next());
        holderPid = row.getInt(1);
      }
      try (var lock = holder.prepareStatement("SELECT id FROM issues WHERE id=? FOR UPDATE")) {
        lock.setObject(1, issue);
        try (var row = lock.executeQuery()) {
          assertTrue(row.next());
        }
      }
      var command =
          pool.submit(
              () ->
                  command(managerSession, action, UUID.randomUUID(), payload)
                      .andReturn()
                      .getResponse()
                      .getStatus());
      try {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        boolean blocked = false;
        while (System.nanoTime() < deadline) {
          blocked =
              Boolean.TRUE.equals(
                  db.queryForObject(
                      "SELECT EXISTS(SELECT 1 FROM pg_stat_activity WHERE"
                          + " ?=ANY(pg_blocking_pids(pid)))",
                      Boolean.class,
                      holderPid));
          if (blocked) break;
          db.queryForList("SELECT pg_sleep(0.01)");
        }
        assertTrue(blocked, "The command must reach the held issue lock before expiring its role");
        // Set the deadline only after observing the lock wait, avoiding a CI scheduling race.
        db.update(
            "UPDATE building_memberships SET valid_to=clock_timestamp()+INTERVAL '200 milliseconds'"
                + " WHERE user_id=? AND building_id=? AND role=?",
            expiringUser,
            building,
            role);
        db.queryForList(
            "SELECT pg_sleep(GREATEST(0,EXTRACT(EPOCH FROM (valid_to-clock_timestamp())))::double"
                + " precision+0.03) FROM building_memberships WHERE user_id=? AND building_id=? AND"
                + " role=?",
            expiringUser,
            building,
            role);
        assertTrue(
            Boolean.TRUE.equals(
                db.queryForObject(
                    "SELECT valid_to<clock_timestamp() FROM building_memberships WHERE user_id=?"
                        + " AND building_id=? AND role=?",
                    Boolean.class,
                    expiringUser,
                    building,
                    role)));
        holder.rollback();
        return command.get(10, TimeUnit.SECONDS);
      } finally {
        holder.rollback();
      }
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void notificationOwnershipAndReadAreIndependentOfListingAndLogin() throws Exception {
    act(managerSession, "REQUEST_INFO", 0, "Xin xác nhận vị trí gần thang máy.");
    var before = notices(residentSession);
    assertEquals(1, before.size());
    assertNull(before.getFirst().get("read_at"));
    assertNull(notices(login(resident)).getFirst().get("read_at"));
    String id = before.getFirst().get("id").toString();
    mvc.perform(post(notifications() + "/" + id + "/read").session(login(stranger)).with(csrf()))
        .andExpect(status().isNotFound());
    mvc.perform(post(notifications() + "/" + id + "/read").session(residentSession).with(csrf()))
        .andExpect(status().isOk());
    Object read = notices(residentSession).getFirst().get("read_at");
    assertNotNull(read);
    mvc.perform(post(notifications() + "/" + id + "/read").session(residentSession).with(csrf()))
        .andExpect(status().isOk());
    assertEquals(read, notices(residentSession).getFirst().get("read_at"));
  }

  @Test
  void endingReporterMembershipHidesNotificationAndDeniesRead() throws Exception {
    act(managerSession, "REQUEST_INFO", 0, "Xin bổ sung vị trí chính xác.");
    String id = notices(residentSession).getFirst().get("id").toString();
    db.update("UPDATE building_memberships SET status='ENDED' WHERE user_id=?", resident);
    var response =
        mvc.perform(get(notifications()).session(residentSession)).andReturn().getResponse();
    if (response.getStatus() == 200)
      assertEquals(0, JSON.readValue(response.getContentAsString(), List.class).size());
    else assertEquals(403, response.getStatus());
    mvc.perform(post(notifications() + "/" + id + "/read").session(residentSession).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  void internalPriorityReasonIsHiddenFromResidentAndAssignedTechnician() throws Exception {
    var body = body(0, "Ghi chú nội bộ về điều phối vật tư.");
    body.put("priority", "HIGH");
    command(managerSession, "SET_PRIORITY", UUID.randomUUID(), body).andExpect(status().isOk());
    act(managerSession, "ACCEPT", 1, "Đã kiểm tra phạm vi.");
    assign(technician, 2);
    assertTrue(history(managerSession).contains("Ghi chú nội bộ về điều phối vật tư."));
    assertFalse(history(residentSession).contains("Ghi chú nội bộ về điều phối vật tư."));
    assertFalse(history(technicianSession).contains("Ghi chú nội bộ về điều phối vật tư."));
  }

  private void failInserts(String table, String functionName) {
    db.execute(
        "CREATE FUNCTION "
            + functionName
            + "() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.issue_id='"
            + issue
            + "'::uuid THEN RAISE EXCEPTION 'deliberate M4 test failure'; END IF; RETURN NEW; END;"
            + " $$");
    db.execute(
        "CREATE TRIGGER "
            + functionName
            + " BEFORE INSERT ON "
            + table
            + " FOR EACH ROW EXECUTE FUNCTION "
            + functionName
            + "()");
  }

  private void removeFailure(String table, String functionName) {
    db.execute("DROP TRIGGER " + functionName + " ON " + table);
    db.execute("DROP FUNCTION " + functionName + "()");
  }

  private void expectCommandFailure(String action, Map<String, Object> body) throws Exception {
    try {
      assertTrue(
          command(managerSession, action, UUID.randomUUID(), body)
                  .andReturn()
                  .getResponse()
                  .getStatus()
              >= 400);
    } catch (jakarta.servlet.ServletException expected) {
      /* An unhandled database fault also proves the rollback path. */
    }
  }

  @Test
  void failedEventWriteRollsBackDecisionAndNotification() throws Exception {
    failInserts("issue_events", "m4_fail_event");
    try {
      expectCommandFailure("ACCEPT", body(0, "Đã kiểm tra phạm vi."));
      assertEquals(0L, persisted().get("version"));
      assertEquals("SUBMITTED", persisted().get("status"));
      assertEquals(1, count("issue_events"));
      assertEquals(0, count("issue_commands"));
      assertEquals(0, count("issue_notifications"));
    } finally {
      removeFailure("issue_events", "m4_fail_event");
    }
  }

  @Test
  void failedAssignmentWriteRollsBackStatusEventAndReceipt() throws Exception {
    accept();
    failInserts("issue_assignments", "m4_fail_assignment");
    try {
      var body = body(1, "Giao kiểm tra đèn hành lang.");
      body.put("technicianId", technician);
      expectCommandFailure("ASSIGN", body);
      assertEquals("ACCEPTED", persisted().get("status"));
      assertEquals(1L, persisted().get("version"));
      assertEquals(2, count("issue_events"));
      assertEquals(1, count("issue_commands"));
      assertEquals(0, count("issue_assignments"));
    } finally {
      removeFailure("issue_assignments", "m4_fail_assignment");
    }
  }

  @Test
  void failedNotificationWriteRollsBackQuestionAndEvent() throws Exception {
    failInserts("issue_notifications", "m4_fail_notification");
    try {
      expectCommandFailure("REQUEST_INFO", body(0, "Xin xác nhận vị trí đèn."));
      assertEquals("SUBMITTED", persisted().get("status"));
      assertEquals(0L, persisted().get("version"));
      assertEquals(1, count("issue_events"));
      assertEquals(0, count("issue_commands"));
      assertEquals(0, count("issue_notifications"));
    } finally {
      removeFailure("issue_notifications", "m4_fail_notification");
    }
  }

  @Test
  void commandRejectsForgedFieldsMissingVersionAndMissingCsrf() throws Exception {
    var forged = body(0, "Cố giả mạo người thực hiện.");
    forged.put("actorId", stranger);
    command(managerSession, "ACCEPT", UUID.randomUUID(), forged)
        .andExpect(status().isUnprocessableContent());
    command(
            managerSession,
            "ACCEPT",
            UUID.randomUUID(),
            new HashMap<>(Map.of("text", "Thiếu version")))
        .andExpect(status().isUnprocessableContent());
    mvc.perform(
            post(base() + "/" + issue + "/actions/ACCEPT")
                .session(managerSession)
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(body(0, "Thiếu CSRF token."))))
        .andExpect(status().isForbidden());
    assertEquals(0L, persisted().get("version"));
    assertEquals(1, count("issue_events"));
  }

  @Test
  void technicianPickerContainsOnlyEligibleBuildingTechnicians() throws Exception {
    UUID disabled = user("Kỹ thuật viên bị khóa");
    member(disabled, building, "TECHNICIAN");
    db.update("UPDATE users SET status='DISABLED' WHERE id=?", disabled);
    String result =
        mvc.perform(get(base() + "/technicians").session(managerSession))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertTrue(result.contains(technician.toString()));
    assertTrue(result.contains(otherTechnician.toString()));
    assertFalse(result.contains(foreignTechnician.toString()));
    assertFalse(result.contains(disabled.toString()));
    assertFalse(result.contains(resident.toString()));
    mvc.perform(get(base() + "/technicians").session(residentSession))
        .andExpect(status().isForbidden());
  }
}
