package com.civicfix.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.civicfix.backend.accounts.RequestLimits;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class IssuesTests {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Autowired PasswordEncoder encoder;
  @Autowired RequestLimits limits;
  static final JsonMapper JSON = JsonMapper.builder().build();
  static final String PASSWORD = "CivicFix test private passphrase 2026";
  static String passwordHash;
  UUID building,
      otherBuilding,
      apartment,
      otherApartment,
      link,
      resident,
      otherResident,
      manager,
      otherManager,
      admin,
      tech;
  MockHttpSession session, managerSession;

  @BeforeEach
  void prepare() throws Exception {
    limits.clear();
    if (passwordHash == null) passwordHash = encoder.encode(PASSWORD);
    building = building();
    otherBuilding = building();
    resident = user();
    otherResident = user();
    manager = user();
    otherManager = user();
    admin = user();
    tech = user();
    member(resident, building, "RESIDENT");
    member(otherResident, building, "RESIDENT");
    member(manager, building, "MANAGER");
    member(otherManager, otherBuilding, "MANAGER");
    member(admin, building, "ADMIN");
    member(tech, building, "TECHNICIAN");
    apartment = apartment(building, "03");
    otherApartment = apartment(otherBuilding, "03");
    link = link(resident, apartment);
    session = login(resident);
    managerSession = login(manager);
  }

  UUID building() {
    UUID id = UUID.randomUUID();
    db.update(
        "INSERT INTO buildings(id,name,address,time_zone) VALUES (?,'Test"
            + " building','Demo','Asia/Ho_Chi_Minh')",
        id);
    return id;
  }

  UUID user() {
    UUID id = UUID.randomUUID();
    db.update(
        "INSERT INTO users(id,email,display_name,password_hash,status,email_verified_at) VALUES"
            + " (?,?,?,?,'ACTIVE',CURRENT_TIMESTAMP)",
        id,
        id + "@demo.civicfix.local",
        "Resident " + id,
        passwordHash);
    return id;
  }

  void member(UUID u, UUID b, String role) {
    db.update(
        "INSERT INTO building_memberships(id,user_id,building_id,role,status) VALUES"
            + " (?,?,?,?,'ACTIVE')",
        UUID.randomUUID(),
        u,
        b,
        role);
  }

  UUID apartment(UUID b, String unit) {
    UUID id = UUID.randomUUID();
    db.update("INSERT INTO apartments(id,building_id,unit_number) VALUES (?,?,?)", id, b, unit);
    return id;
  }

  UUID link(UUID u, UUID a) {
    UUID id = UUID.randomUUID();
    db.update(
        "INSERT INTO"
            + " resident_apartment_links(id,user_id,apartment_id,verified_by,verification_source)"
            + " VALUES (?,?,?,?,'RESIDENT_LIST')",
        id,
        u,
        a,
        admin);
    return id;
  }

  MockHttpSession login(UUID u) throws Exception {
    return (MockHttpSession)
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .param("email", u + "@demo.civicfix.local")
                    .param("password", PASSWORD))
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession(false);
  }

  String base() {
    return "/api/buildings/" + building + "/issues";
  }

  Map<String, Object> payload() {
    return new HashMap<>(
        Map.of(
            "scope",
            "COMMON_AREA",
            "description",
            "Đèn chớp liên tục vào buổi tối.",
            "locationDescription",
            "Hành lang tầng 3",
            "category",
            "ELECTRICAL"));
  }

  ResultActions send(MockHttpSession s, UUID key, Map<String, Object> body) throws Exception {
    return mvc.perform(
        post(base())
            .session(s)
            .with(csrf())
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(JSON.writeValueAsString(body)));
  }

  Map<String, Object> create() throws Exception {
    return JSON.readValue(
        send(session, UUID.randomUUID(), payload())
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString(),
        Map.class);
  }

  int count(String table) {
    return db.queryForObject(
        "SELECT count(*) FROM "
            + table
            + " WHERE "
            + (table.equals("issues")
                ? "building_id=?"
                : "issue_id IN (SELECT id FROM issues WHERE building_id=?)"),
        Integer.class,
        building);
  }

  @Test
  void residentCreatesSnapshotAndInitialEvent() throws Exception {
    var i = create();
    assertEquals("SUBMITTED", i.get("status"));
    assertEquals(resident.toString(), i.get("reported_by"));
    assertEquals("03", i.get("apartment_label_at_report"));
    assertEquals(apartment.toString(), i.get("apartment_id_at_report"));
    assertEquals("Hành lang tầng 3", i.get("location_description"));
    assertTrue(i.get("code").toString().startsWith("CF-"));
    assertFalse(i.containsKey("request_hash"));
    assertEquals(1, count("issues"));
    assertEquals(1, count("issue_events"));
    mvc.perform(get(base() + "/" + i.get("id") + "/events").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].event_type").value("ISSUE_SUBMITTED"));
  }

  @Test
  void noVerifiedApartmentStillReports() throws Exception {
    var i =
        JSON.readValue(
            send(login(otherResident), UUID.randomUUID(), payload())
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString(),
            Map.class);
    assertNull(i.get("apartment_id_at_report"));
    assertNull(i.get("apartment_label_at_report"));
  }

  @Test
  void multipleApartmentsRequireOwnedChoice() throws Exception {
    UUID a = apartment(building, "04"), l = link(resident, a);
    send(session, UUID.randomUUID(), payload()).andExpect(status().isUnprocessableContent());
    var p = payload();
    p.put("apartmentLinkId", l);
    send(session, UUID.randomUUID(), p)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.apartment_label_at_report").value("04"));
  }

  @Test
  void rejectsSomeoneElsesOrForeignOrEndedLink() throws Exception {
    UUID foreign = link(otherResident, otherApartment), someone = link(otherResident, apartment);
    for (UUID bad : List.of(foreign, someone)) {
      var p = payload();
      p.put("apartmentLinkId", bad);
      send(session, UUID.randomUUID(), p).andExpect(status().isUnprocessableContent());
    }
    db.update(
        "UPDATE resident_apartment_links SET status='ENDED',ended_at=CURRENT_TIMESTAMP WHERE id=?",
        link);
    var p = payload();
    p.put("apartmentLinkId", link);
    send(session, UUID.randomUUID(), p).andExpect(status().isUnprocessableContent());
    assertEquals(0, count("issues"));
  }

  @Test
  void rejectsForgedActorApartmentStatusBuildingAndAttachments() throws Exception {
    for (String field :
        List.of(
            "reported_by",
            "apartment_id_at_report",
            "building_id",
            "status",
            "assigned_to",
            "attachments")) {
      var p = payload();
      p.put(field, "forged");
      send(session, UUID.randomUUID(), p).andExpect(status().isUnprocessableContent());
    }
    assertEquals(0, count("issues"));
  }

  @Test
  void rejectsInvalidAndPrivateRequests() throws Exception {
    for (String field : List.of("locationDescription", "description", "scope", "category")) {
      var p = payload();
      p.put(field, " ");
      send(session, UUID.randomUUID(), p).andExpect(status().isUnprocessableContent());
    }
    var p = payload();
    p.put("scope", "PRIVATE_APARTMENT");
    send(session, UUID.randomUUID(), p).andExpect(status().isUnprocessableContent());
    p = payload();
    p.put("description", "x".repeat(3001));
    send(session, UUID.randomUUID(), p).andExpect(status().isUnprocessableContent());
    assertEquals(0, count("issues"));
  }

  @Test
  void sameKeyRetriesReturnSameIssue() throws Exception {
    UUID key = UUID.randomUUID();
    var p = payload();
    String first =
        send(session, key, p)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String second =
        send(session, key, p)
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertEquals(first, second);
    assertEquals(1, count("issues"));
    assertEquals(1, count("issue_events"));
  }

  @Test
  void changedPayloadSameKeyConflicts() throws Exception {
    UUID key = UUID.randomUUID();
    send(session, key, payload()).andExpect(status().isOk());
    var p = payload();
    p.put("description", "Một sự cố khác có nội dung mới");
    send(session, key, p).andExpect(status().isConflict());
    assertEquals(1, count("issues"));
  }

  @Test
  void concurrentRetriesCreateOneIssueAndOneEvent() throws Exception {
    UUID key = UUID.randomUUID();
    var secondSession = login(resident);
    var pool = Executors.newFixedThreadPool(2);
    var start = new CountDownLatch(1);
    try {
      List<Future<String>> futures = new ArrayList<>();
      for (var s : List.of(session, secondSession))
        futures.add(
            pool.submit(() -> {
              start.await();
              return send(s, key, payload())
                  .andExpect(status().isOk())
                  .andReturn()
                  .getResponse()
                  .getContentAsString();
            }));
      start.countDown();
      assertEquals(
          futures.get(0).get(10, TimeUnit.SECONDS), futures.get(1).get(10, TimeUnit.SECONDS));
      assertEquals(1, count("issues"));
      assertEquals(1, count("issue_events"));
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void sameKeyIsScopedToReporterAndBuilding() throws Exception {
    UUID key = UUID.randomUUID();
    send(session, key, payload()).andExpect(status().isOk());
    send(login(otherResident), key, payload()).andExpect(status().isOk());
    assertEquals(2, count("issues"));
  }

  @Test
  void residentCannotReadOtherResidentsDetailHistoryOrList() throws Exception {
    var i = create();
    var other = login(otherResident);
    for (String path :
        List.of("/" + i.get("id"), "/" + i.get("id") + "/events", "/" + UUID.randomUUID()))
      mvc.perform(get(base() + path).session(other)).andExpect(status().isNotFound());
    mvc.perform(get(base()).session(other))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));
  }

  @Test
  void managerCanReadBuildingAndSeesVerifiedReporter() throws Exception {
    var i = create();
    mvc.perform(get(base()).session(managerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].reported_by").value(resident.toString()))
        .andExpect(jsonPath("$.items[0].apartment_label_at_report").value("03"));
    mvc.perform(get(base() + "/" + i.get("id")).session(managerSession)).andExpect(status().isOk());
    mvc.perform(get(base() + "/" + i.get("id") + "/events").session(managerSession))
        .andExpect(status().isOk());
  }

  @Test
  void managerCanSubmitOwnReportWithoutResidentRole() throws Exception {
    send(managerSession, UUID.randomUUID(), payload())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reported_by").value(manager.toString()))
        .andExpect(jsonPath("$.apartment_id_at_report").doesNotExist());
  }

  @Test
  void adminAndUnassignedTechnicianCannotAccessIssues() throws Exception {
    var i = create();
    for (UUID user : List.of(admin, tech)) {
      var s = login(user);
      send(s, UUID.randomUUID(), payload()).andExpect(status().isForbidden());
      if (user.equals(tech)) {
        mvc.perform(get(base()).session(s))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(0));
      } else {
        mvc.perform(get(base()).session(s)).andExpect(status().isForbidden());
      }
      mvc.perform(get(base() + "/" + i.get("id")).session(s)).andExpect(status().isNotFound());
      mvc.perform(get(base() + "/" + i.get("id") + "/events").session(s))
          .andExpect(status().isNotFound());
    }
  }

  @Test
  void managerFromOtherBuildingCannotReadOrSubmit() throws Exception {
    var i = create();
    var s = login(otherManager);
    send(s, UUID.randomUUID(), payload()).andExpect(status().isForbidden());
    mvc.perform(get(base()).session(s)).andExpect(status().isForbidden());
    mvc.perform(get(base() + "/" + i.get("id")).session(s)).andExpect(status().isNotFound());
    mvc.perform(get("/api/buildings/" + otherBuilding + "/issues/" + i.get("id")).session(s))
        .andExpect(status().isNotFound());
  }

  @Test
  void effectiveRolesAreAUnionInSameBuilding() throws Exception {
    member(tech, building, "MANAGER");
    var i = create();
    mvc.perform(get(base() + "/" + i.get("id")).session(login(tech))).andExpect(status().isOk());
  }

  @Test
  void endingMembershipDeniesExistingSessionButRetainsManagerHistory() throws Exception {
    var i = create();
    db.update("UPDATE building_memberships SET status='ENDED' WHERE user_id=?", resident);
    mvc.perform(get(base() + "/" + i.get("id")).session(session)).andExpect(status().isNotFound());
    send(session, UUID.randomUUID(), payload()).andExpect(status().isForbidden());
    mvc.perform(get(base() + "/" + i.get("id")).session(managerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.apartment_label_at_report").value("03"));
  }

  @Test
  void futureAndExpiredMembershipCannotCreate() throws Exception {
    db.update(
        "UPDATE building_memberships SET valid_from=CURRENT_TIMESTAMP+INTERVAL '1 day' WHERE"
            + " user_id=?",
        resident);
    send(session, UUID.randomUUID(), payload()).andExpect(status().isForbidden());
    db.update(
        "UPDATE building_memberships SET valid_from=CURRENT_TIMESTAMP-INTERVAL '2"
            + " days',valid_to=CURRENT_TIMESTAMP-INTERVAL '1 day' WHERE user_id=?",
        resident);
    send(session, UUID.randomUUID(), payload()).andExpect(status().isForbidden());
  }

  @Test
  void disabledAccountCannotReuseSession() throws Exception {
    create();
    db.update("UPDATE users SET status='DISABLED' WHERE id=?", resident);
    mvc.perform(get(base()).session(session)).andExpect(status().isUnauthorized());
  }

  @Test
  void apartmentTransferAndRenamePreserveOldSnapshot() throws Exception {
    var old = create();
    db.update(
        "UPDATE resident_apartment_links SET status='ENDED',ended_at=CURRENT_TIMESTAMP WHERE id=?",
        link);
    UUID next = apartment(building, "04");
    link(resident, next);
    db.update("UPDATE apartments SET unit_number='03-renamed' WHERE id=?", apartment);
    var latest = create();
    assertEquals("04", latest.get("apartment_label_at_report"));
    mvc.perform(get(base() + "/" + old.get("id")).session(managerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.apartment_label_at_report").value("03"));
  }

  @Test
  void replayAfterApartmentChangeReturnsOriginalSnapshot() throws Exception {
    UUID key = UUID.randomUUID();
    send(session, key, payload()).andExpect(status().isOk());
    db.update(
        "UPDATE resident_apartment_links SET status='ENDED',ended_at=CURRENT_TIMESTAMP WHERE id=?",
        link);
    send(session, key, payload())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.apartment_label_at_report").value("03"));
    assertEquals(1, count("issues"));
  }

  @Test
  void failureWritingHistoryRollsBackIssue() throws Exception {
    db.execute(
        "CREATE FUNCTION fail_m3_event() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN RAISE"
            + " EXCEPTION 'test history failure'; END; $$");
    db.execute(
        "CREATE TRIGGER test_m3_history_failure BEFORE INSERT ON issue_events FOR EACH ROW EXECUTE"
            + " FUNCTION fail_m3_event()");
    try {
      assertThrows(Exception.class, () -> send(session, UUID.randomUUID(), payload()).andReturn());
      assertEquals(0, count("issues"));
      assertEquals(0, count("issue_events"));
    } finally {
      db.execute("DROP TRIGGER test_m3_history_failure ON issue_events");
      db.execute("DROP FUNCTION fail_m3_event()");
    }
  }

  @Test
  void historyCannotBeOverwritten() throws Exception {
    var i = create();
    assertThrows(
        org.springframework.dao.DataAccessException.class,
        () ->
            db.update(
                "UPDATE issue_events SET public_message='changed' WHERE issue_id=?",
                UUID.fromString(i.get("id").toString())));
    assertThrows(
        org.springframework.dao.DataAccessException.class,
        () ->
            db.update(
                "DELETE FROM issue_events WHERE issue_id=?",
                UUID.fromString(i.get("id").toString())));
  }

  @Test
  void paginationAndSearchStayInScope() throws Exception {
    for (int n = 0; n < 3; n++) create();
    mvc.perform(get(base() + "?size=2").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.hasMore").value(true));
    mvc.perform(get(base() + "?size=2&page=1").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.hasMore").value(false));
    mvc.perform(get(base() + "?q=not-existing").session(managerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));
    mvc.perform(get(base() + "?q=%25").session(managerSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(0));
    for (String query : List.of("size=51", "page=-1", "size=0"))
      mvc.perform(get(base() + "?" + query).session(session))
          .andExpect(status().isUnprocessableContent());
  }

  @Test
  void idempotencyHeaderAndTypedParametersAreValidated() throws Exception {
    for (String invalid : List.of("", "not-a-uuid")) {
      var req =
          post(base())
              .session(session)
              .with(csrf())
              .contentType(MediaType.APPLICATION_JSON)
              .content(JSON.writeValueAsString(payload()));
      if (!invalid.isEmpty()) req.header("Idempotency-Key", invalid);
      mvc.perform(req).andExpect(status().isUnprocessableContent());
    }
    mvc.perform(get(base() + "?page=not-a-number").session(session))
        .andExpect(status().isUnprocessableContent());
    assertEquals(0, count("issues"));
  }

  @Test
  void csrfAndLoginAreRequired() throws Exception {
    mvc.perform(get(base())).andExpect(status().isUnauthorized());
    mvc.perform(
            post(base())
                .session(session)
                .header("Idempotency-Key", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(payload())))
        .andExpect(status().isForbidden());
    assertEquals(0, count("issues"));
  }
}
