package com.civicfix.backend;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.civicfix.backend.accounts.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AccountLifecycleTests {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate db;
  @Autowired PasswordEncoder encoder;
  @Autowired RequestLimits limits;
  @MockitoBean LocalMailService mail;
  static final String PASSWORD = "A private demo passphrase 2026";
  static final String NEW_PASSWORD = "Another private passphrase for 2026";
  static final JsonMapper JSON = JsonMapper.builder().build();
  UUID building, otherBuilding, apartment, otherApartment, admin, resident, manager, technician;
  String invitedEmail, adminEmail, residentEmail;
  MockHttpSession adminSession;

  @BeforeEach
  void prepare() throws Exception {
    limits.clear();
    building = building();
    otherBuilding = building();
    apartment = apartment(building, "03");
    otherApartment = apartment(otherBuilding, "03");
    admin = user("ACTIVE");
    resident = user("ACTIVE");
    manager = user("ACTIVE");
    technician = user("ACTIVE");
    member(admin, building, "ADMIN");
    member(resident, building, "RESIDENT");
    member(manager, building, "MANAGER");
    member(technician, building, "TECHNICIAN");
    adminEmail = email(admin);
    residentEmail = email(resident);
    invitedEmail = UUID.randomUUID() + "@demo.civicfix.local";
    adminSession = login(adminEmail, PASSWORD);
  }

  UUID building() {
    UUID id = UUID.randomUUID();
    db.update(
        "INSERT INTO buildings(id,name,address,time_zone) VALUES (?,'Test building','Demo"
            + " address','Asia/Ho_Chi_Minh')",
        id);
    return id;
  }

  UUID apartment(UUID b, String unit) {
    UUID id = UUID.randomUUID();
    db.update("INSERT INTO apartments(id,building_id,unit_number) VALUES (?,?,?)", id, b, unit);
    return id;
  }

  UUID user(String state) {
    UUID id = UUID.randomUUID();
    db.update(
        "INSERT INTO users(id,email,display_name,password_hash,status,email_verified_at) VALUES"
            + " (?,?,?, ?,?,CURRENT_TIMESTAMP)",
        id,
        email(id),
        "Demo user",
        encoder.encode(PASSWORD),
        state);
    return id;
  }

  String email(UUID id) {
    return id + "@demo.civicfix.local";
  }

  UUID member(UUID user, UUID b, String role) {
    UUID id = UUID.randomUUID();
    db.update(
        "INSERT INTO building_memberships(id,user_id,building_id,role,status) VALUES"
            + " (?,?,?,?,'ACTIVE')",
        id,
        user,
        b,
        role);
    return id;
  }

  MockHttpSession login(String email, String password) throws Exception {
    var r =
        mvc.perform(
                post("/api/auth/login")
                    .with(csrf())
                    .param("email", email)
                    .param("password", password))
            .andExpect(status().isOk())
            .andReturn();
    return (MockHttpSession) r.getRequest().getSession(false);
  }

  ResultActions send(String path, MockHttpSession session, Map<String, ?> data) throws Exception {
    var req =
        post(path)
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(JSON.writeValueAsString(data));
    if (session != null) req.session(session);
    return mvc.perform(req);
  }

  Map<String, Object> inviteData(String email, UUID apt) {
    return Map.of(
        "email",
        email,
        "displayName",
        "Cư dân thử nghiệm",
        "apartmentId",
        apt,
        "verificationSource",
        "RESIDENT_LIST");
  }

  String base() {
    return "/api/buildings/" + building;
  }

  UUID invite(String email) throws Exception {
    var r =
        send(base() + "/invitations", adminSession, inviteData(email, apartment))
            .andExpect(status().isOk())
            .andReturn();
    return UUID.fromString(
        JSON.readTree(r.getResponse().getContentAsString()).get("id").asString());
  }

  String tokenFor(String email) {
    long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
    while (System.nanoTime() < deadline
        && mockingDetails(mail).getInvocations().stream()
            .noneMatch(i ->
                email.equals(i.getArgument(0))
                    && ((String) i.getArgument(2)).contains("#token="))) {
      try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new RuntimeException(e);
      }
    }
    return mockingDetails(mail).getInvocations().stream()
        .filter(i -> i.getMethod().getName().equals("send") && email.equals(i.getArgument(0)))
        .map(i -> (String) i.getArgument(2))
        .filter(s -> s.contains("#token="))
        .reduce((a, b) -> b)
        .map(s -> {
          Matcher m = Pattern.compile("#token=([A-Za-z0-9_-]+)").matcher(s);
          assertTrue(m.find());
          return m.group(1);
        })
        .orElseThrow();
  }

  void accept(String token) throws Exception {
    send("/api/invitations/accept", null, Map.of("token", token, "password", PASSWORD))
        .andExpect(status().isOk());
  }

  long count(String table, String where, Object... args) {
    return db.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + where, Long.class, args);
  }

  @Test
  void loginPersistsSessionAndMeDoesNotExposeSecrets() throws Exception {
    mvc.perform(get("/api/auth/me").session(adminSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value(adminEmail))
        .andExpect(jsonPath("$.password_hash").doesNotExist())
        .andExpect(jsonPath("$.memberships[0].role").value("ADMIN"));
  }

  @Test
  void loginNeedsCsrf() throws Exception {
    mvc.perform(post("/api/auth/login").param("email", residentEmail).param("password", PASSWORD))
        .andExpect(status().isForbidden());
  }

  @Test
  void loginFailuresAreGenericForMissingInactiveAndWrongPassword() throws Exception {
    String body = null;
    for (String address :
        List.of(
            email(UUID.randomUUID()),
            email(user("INVITED")),
            email(user("DISABLED")),
            residentEmail)) {
      var r =
          mvc.perform(
                  post("/api/auth/login")
                      .with(csrf())
                      .param("email", address)
                      .param("password", "wrong password"))
              .andExpect(status().isUnauthorized())
              .andReturn();
      if (body == null) body = r.getResponse().getContentAsString();
      else assertEquals(body, r.getResponse().getContentAsString());
      var failedSession = r.getRequest().getSession(false);
      assertTrue(
          failedSession == null || failedSession.getAttribute("SPRING_SECURITY_CONTEXT") == null);
    }
  }

  @Test
  void correctPasswordCannotLoginDisabledUser() throws Exception {
    mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .param("email", email(user("DISABLED")))
                .param("password", PASSWORD))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void repeatedLoginIsRateLimited() throws Exception {
    for (int n = 0; n < 15; n++)
      mvc.perform(
              post("/api/auth/login")
                  .with(csrf())
                  .param("email", residentEmail)
                  .param("password", "wrong"))
          .andExpect(status().isUnauthorized());
    mvc.perform(
            post("/api/auth/login")
                .with(csrf())
                .param("email", residentEmail)
                .param("password", "wrong"))
        .andExpect(status().isTooManyRequests());
  }

  @Test
  void anonymousCannotReadAdminAccounts() throws Exception {
    mvc.perform(get(base() + "/accounts")).andExpect(status().isUnauthorized());
  }

  @Test
  void allNonAdminRolesCannotInvite() throws Exception {
    for (UUID u : List.of(resident, manager, technician))
      send(base() + "/invitations", login(email(u), PASSWORD), inviteData(invitedEmail, apartment))
          .andExpect(status().isForbidden());
    assertEquals(0, count("users", "email=?", invitedEmail));
    verifyNoInteractions(mail);
  }

  @Test
  void adminCannotOperateOtherBuilding() throws Exception {
    send(
            "/api/buildings/" + otherBuilding + "/invitations",
            adminSession,
            inviteData(invitedEmail, otherApartment))
        .andExpect(status().isForbidden());
    verifyNoInteractions(mail);
  }

  @Test
  void foreignApartmentIsRejected() throws Exception {
    send(base() + "/invitations", adminSession, inviteData(invitedEmail, otherApartment))
        .andExpect(status().isNotFound());
    verifyNoInteractions(mail);
  }

  @Test
  void inviteCreatesOnlyPendingMembershipAndHash() throws Exception {
    UUID id = invite(invitedEmail);
    String token = tokenFor(invitedEmail);
    var row = db.queryForMap("SELECT * FROM account_invitations WHERE id=?", id);
    assertNotEquals(token, row.get("token_hash"));
    assertEquals(64, ((String) row.get("token_hash")).length());
    assertEquals(
        1, count("building_memberships", "id=? AND status='PENDING'", row.get("membership_id")));
    assertEquals(0, count("resident_apartment_links", "user_id=?", row.get("user_id")));
    mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
  }

  @Test
  void duplicateClickSendsOneEmailAndCreatesOneInvitation() throws Exception {
    UUID first = invite(invitedEmail), second = invite(invitedEmail.toUpperCase(Locale.ROOT));
    assertEquals(first, second);
    verify(mail, times(1)).send(eq(invitedEmail), anyString(), anyString());
  }

  @Test
  void concurrentDuplicateInvitesAreIdempotent() throws Exception {
    try (var pool = Executors.newFixedThreadPool(2)) {
      var results = pool.invokeAll(List.of(() -> invite(invitedEmail), () -> invite(invitedEmail)));
      assertEquals(results.get(0).get(), results.get(1).get());
    }
    verify(mail, times(1)).send(eq(invitedEmail), anyString(), anyString());
  }

  @Test
  void failedMailRollsBackAccountAndInvitation() throws Exception {
    doThrow(new ApiErrors.Problem(503, "Mail unavailable"))
        .when(mail)
        .send(anyString(), anyString(), anyString());
    send(base() + "/invitations", adminSession, inviteData(invitedEmail, apartment))
        .andExpect(status().isServiceUnavailable());
    assertEquals(0, count("users", "email=?", invitedEmail));
  }

  @Test
  void activationCreatesVerifiedApartmentAndCannotBeRepeated() throws Exception {
    invite(invitedEmail);
    String token = tokenFor(invitedEmail);
    accept(token);
    send("/api/invitations/accept", null, Map.of("token", token, "password", PASSWORD))
        .andExpect(status().isGone());
    var session = login(invitedEmail, PASSWORD);
    mvc.perform(get("/api/auth/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.apartments[0].unit_number").value("03"));
  }

  @Test
  void concurrentActivationHasOnlyOneWinner() throws Exception {
    invite(invitedEmail);
    String token = tokenFor(invitedEmail);
    try (var pool = Executors.newFixedThreadPool(2)) {
      Callable<Integer> request =
          () ->
              send("/api/invitations/accept", null, Map.of("token", token, "password", PASSWORD))
                  .andReturn()
                  .getResponse()
                  .getStatus();
      var results = pool.invokeAll(List.of(request, request));
      var codes =
          results.stream()
              .map(f -> {
                try {
                  return f.get();
                } catch (Exception e) {
                  throw new RuntimeException(e);
                }
              })
              .sorted()
              .toList();
      assertEquals(List.of(200, 410), codes);
    }
  }

  @Test
  void shortPasswordDoesNotConsumeInvitation() throws Exception {
    invite(invitedEmail);
    String token = tokenFor(invitedEmail);
    send("/api/invitations/accept", null, Map.of("token", token, "password", "short"))
        .andExpect(status().isUnprocessableContent());
    accept(token);
  }

  @Test
  void longUnicodePasswordCanBeUsed() throws Exception {
    invite(invitedEmail);
    String password =
        "Một cụm mật khẩu rất dài và có thể dán từ trình quản lý mật khẩu " + "á".repeat(25);
    send(
            "/api/invitations/accept",
            null,
            Map.of("token", tokenFor(invitedEmail), "password", password))
        .andExpect(status().isOk());
    login(invitedEmail, password);
  }

  @Test
  void expiredInvitationCannotActivate() throws Exception {
    UUID id = invite(invitedEmail);
    String token = tokenFor(invitedEmail);
    db.update(
        "UPDATE account_invitations SET expires_at=CURRENT_TIMESTAMP-INTERVAL '1 minute' WHERE"
            + " id=?",
        id);
    send("/api/invitations/preview", null, Map.of("token", token)).andExpect(status().isGone());
    send("/api/invitations/accept", null, Map.of("token", token, "password", PASSWORD))
        .andExpect(status().isGone());
  }

  @Test
  void resendInvalidatesOldLinkAndEnforcesCooldown() throws Exception {
    UUID id = invite(invitedEmail);
    String old = tokenFor(invitedEmail);
    send(base() + "/invitations/" + id + "/resend", adminSession, Map.of())
        .andExpect(status().isTooManyRequests());
    db.update(
        "UPDATE account_invitations SET last_sent_at=CURRENT_TIMESTAMP-INTERVAL '2 minutes' WHERE"
            + " id=?",
        id);
    send(base() + "/invitations/" + id + "/resend", adminSession, Map.of())
        .andExpect(status().isOk());
    String latest = tokenFor(invitedEmail);
    assertNotEquals(old, latest);
    send("/api/invitations/preview", null, Map.of("token", old)).andExpect(status().isGone());
    accept(latest);
  }

  @Test
  void revokedLinkCannotActivate() throws Exception {
    UUID id = invite(invitedEmail);
    String token = tokenFor(invitedEmail);
    send(
            base() + "/invitations/" + id + "/revoke",
            adminSession,
            Map.of("reason", "Mời nhầm email"))
        .andExpect(status().isOk());
    send("/api/invitations/accept", null, Map.of("token", token, "password", PASSWORD))
        .andExpect(status().isGone());
  }

  @Test
  void wrongApartmentReportStopsActivationAndAppearsForAdmin() throws Exception {
    UUID id = invite(invitedEmail);
    String token = tokenFor(invitedEmail);
    send("/api/invitations/report-incorrect", null, Map.of("token", token))
        .andExpect(status().isOk());
    assertEquals(
        1, count("account_invitations", "id=? AND incorrect_details AND status='REVOKED'", id));
    send("/api/invitations/accept", null, Map.of("token", token, "password", PASSWORD))
        .andExpect(status().isGone());
    assertEquals(
        1,
        count("account_audit_events", "target_id=? AND action='INVITATION_DETAILS_REPORTED'", id));
  }

  @Test
  void existingUserMustAcceptAsSameIdentityAndPasswordIsUnchanged() throws Exception {
    String before =
        db.queryForObject("SELECT password_hash FROM users WHERE id=?", String.class, resident);
    invite(residentEmail);
    String token = tokenFor(residentEmail);
    send("/api/invitations/accept", adminSession, Map.of("token", token))
        .andExpect(status().isForbidden());
    send("/api/invitations/accept", null, Map.of("token", token)).andExpect(status().isForbidden());
    send("/api/invitations/accept", login(residentEmail, PASSWORD), Map.of("token", token))
        .andExpect(status().isOk());
    assertEquals(
        before,
        db.queryForObject("SELECT password_hash FROM users WHERE id=?", String.class, resident));
  }

  @Test
  void disabledUserCannotBeReactivatedByInvitation() throws Exception {
    UUID id = invite(invitedEmail);
    String token = tokenFor(invitedEmail);
    db.update(
        "UPDATE users SET status='DISABLED' WHERE id=(SELECT user_id FROM account_invitations WHERE"
            + " id=?)",
        id);
    send("/api/invitations/accept", null, Map.of("token", token, "password", PASSWORD))
        .andExpect(status().isGone());
  }

  @Test
  void forgotPasswordResponsesAreEquivalent() throws Exception {
    var a =
        send("/api/auth/forgot-password", null, Map.of("email", residentEmail))
            .andExpect(status().isOk())
            .andReturn();
    var b =
        send("/api/auth/forgot-password", null, Map.of("email", invitedEmail))
            .andExpect(status().isOk())
            .andReturn();
    assertEquals(a.getResponse().getContentAsString(), b.getResponse().getContentAsString());
    tokenFor(residentEmail);
    verify(mail, times(1)).send(anyString(), anyString(), anyString());
  }

  @Test
  void recoveryMailFailureDoesNotDiscloseAccountExistence() throws Exception {
    doThrow(new ApiErrors.Problem(503, "Mail unavailable"))
        .when(mail)
        .send(anyString(), anyString(), anyString());
    var known =
        send("/api/auth/forgot-password", null, Map.of("email", residentEmail))
            .andExpect(status().isOk())
            .andReturn();
    var missing =
        send("/api/auth/forgot-password", null, Map.of("email", invitedEmail))
            .andExpect(status().isOk())
            .andReturn();
    assertEquals(
        known.getResponse().getContentAsString(), missing.getResponse().getContentAsString());
    verify(mail, timeout(3000)).send(eq(residentEmail), anyString(), anyString());
  }

  @Test
  void passwordResetInvalidatesSessionsAndAllResetTokens() throws Exception {
    MockHttpSession session = login(residentEmail, PASSWORD);
    send("/api/auth/forgot-password", null, Map.of("email", residentEmail))
        .andExpect(status().isOk());
    String token = tokenFor(residentEmail);
    send("/api/auth/reset-password", null, Map.of("token", token, "password", NEW_PASSWORD))
        .andExpect(status().isOk());
    send("/api/auth/reset-password", null, Map.of("token", token, "password", NEW_PASSWORD))
        .andExpect(status().isGone());
    mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    login(residentEmail, NEW_PASSWORD);
    verify(mail).send(eq(residentEmail), eq("Mật khẩu CivicFix đã được đổi"), anyString());
  }

  @Test
  void passwordResetDoesNotActivatePendingMembership() throws Exception {
    invite(invitedEmail);
    send("/api/auth/forgot-password", null, Map.of("email", invitedEmail))
        .andExpect(status().isOk());
    assertEquals(
        0,
        count(
            "password_reset_tokens", "user_id=(SELECT id FROM users WHERE email=?)", invitedEmail));
    assertEquals(
        1,
        count(
            "building_memberships",
            "user_id=(SELECT id FROM users WHERE email=?) AND status='PENDING'",
            invitedEmail));
  }

  @Test
  void expiredResetTokenIsRejected() throws Exception {
    send("/api/auth/forgot-password", null, Map.of("email", residentEmail))
        .andExpect(status().isOk());
    String token = tokenFor(residentEmail);
    db.update(
        "UPDATE password_reset_tokens SET expires_at=CURRENT_TIMESTAMP-INTERVAL '1 minute' WHERE"
            + " user_id=?",
        resident);
    send("/api/auth/reset-password", null, Map.of("token", token, "password", NEW_PASSWORD))
        .andExpect(status().isGone());
  }

  @Test
  void logoutRemovesAuthentication() throws Exception {
    var session = login(residentEmail, PASSWORD);
    send("/api/auth/logout", session, Map.of()).andExpect(status().isOk());
    mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    assertTrue(session.isInvalid());
  }

  @Test
  void endingMembershipRevokesOldSessionAndApartmentLinks() throws Exception {
    var session = login(residentEmail, PASSWORD);
    UUID link = UUID.randomUUID();
    db.update(
        "INSERT INTO"
            + " resident_apartment_links(id,user_id,apartment_id,verified_by,verification_source)"
            + " VALUES (?,?,?,?,'RESIDENT_LIST')",
        link,
        resident,
        apartment,
        admin);
    UUID membership =
        db.queryForObject(
            "SELECT id FROM building_memberships WHERE user_id=? AND role='RESIDENT'",
            UUID.class,
            resident);
    send(
            base() + "/memberships/" + membership + "/end",
            adminSession,
            Map.of("reason", "Chuyển đi"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    assertEquals(1, count("resident_apartment_links", "id=? AND status='ENDED'", link));
  }

  @Test
  void disablingAccountRevokesExistingSession() throws Exception {
    var session = login(residentEmail, PASSWORD);
    send(
            base() + "/accounts/" + resident + "/disable",
            adminSession,
            Map.of("reason", "Khóa thử nghiệm"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
  }

  @Test
  void cannotDisableAccountWithMembershipElsewhere() throws Exception {
    member(resident, otherBuilding, "RESIDENT");
    send(
            base() + "/accounts/" + resident + "/disable",
            adminSession,
            Map.of("reason", "Sai phạm vi"))
        .andExpect(status().isConflict());
    assertEquals(
        "ACTIVE", db.queryForObject("SELECT status FROM users WHERE id=?", String.class, resident));
  }

  @Test
  void workspaceDoesNotGrantOtherRoles() throws Exception {
    var residentSession = login(residentEmail, PASSWORD);
    mvc.perform(get(base() + "/workspace/RESIDENT").session(residentSession))
        .andExpect(status().isOk());
    mvc.perform(get(base() + "/workspace/MANAGER").session(residentSession))
        .andExpect(status().isForbidden());
    mvc.perform(get(base() + "/workspace/MANAGER").session(adminSession))
        .andExpect(status().isForbidden());
    mvc.perform(
            get("/api/buildings/" + otherBuilding + "/workspace/RESIDENT").session(residentSession))
        .andExpect(status().isForbidden());
  }

  @Test
  void expiredMembershipDoesNotGrantRole() throws Exception {
    db.update(
        "UPDATE building_memberships SET valid_from=CURRENT_TIMESTAMP-INTERVAL '2"
            + " days',valid_to=CURRENT_TIMESTAMP-INTERVAL '1 day' WHERE user_id=?",
        resident);
    mvc.perform(get(base() + "/workspace/RESIDENT").session(login(residentEmail, PASSWORD)))
        .andExpect(status().isForbidden());
  }

  @Test
  void forgedVerifierCannotOverrideServerIdentity() throws Exception {
    var data = new HashMap<>(inviteData(invitedEmail, apartment));
    data.put("invitedBy", resident.toString());
    data.put("role", "ADMIN");
    send(base() + "/invitations", adminSession, data).andExpect(status().isOk());
    assertEquals(
        1,
        count(
            "account_invitations",
            "user_id=(SELECT id FROM users WHERE email=?) AND invited_by=?",
            invitedEmail,
            admin));
    assertEquals(
        "RESIDENT",
        db.queryForObject(
            "SELECT role FROM building_memberships WHERE user_id=(SELECT id FROM users WHERE"
                + " email=?)",
            String.class,
            invitedEmail));
  }

  @Test
  void sessionIdIsRotatedOnLogin() throws Exception {
    var session = new MockHttpSession();
    String old = session.getId();
    var result =
        mvc.perform(
                post("/api/auth/login")
                    .session(session)
                    .with(request -> {
                      request.setRequestedSessionId(old);
                      request.setRequestedSessionIdValid(true);
                      return request;
                    })
                    .with(csrf())
                    .param("email", residentEmail)
                    .param("password", PASSWORD))
            .andExpect(status().isOk())
            .andReturn();
    assertNotEquals(old, result.getRequest().getSession().getId());
  }

  @Test
  void changingApartmentRevokesSessionAndKeepsHistoricalLink() throws Exception {
    UUID link = UUID.randomUUID();
    db.update(
        "INSERT INTO"
            + " resident_apartment_links(id,user_id,apartment_id,verified_by,verification_source)"
            + " VALUES (?,?,?,?,'RESIDENT_LIST')",
        link,
        resident,
        apartment,
        admin);
    var session = login(residentEmail, PASSWORD);
    UUID next = apartment(building, "04");
    send(base() + "/links/" + link + "/end", adminSession, Map.of("reason", "Chuyển căn hộ"))
        .andExpect(status().isOk());
    send(
            base() + "/links",
            adminSession,
            Map.of("userId", resident, "apartmentId", next, "verificationSource", "RESIDENT_LIST"))
        .andExpect(status().isOk());
    mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/auth/me").session(login(residentEmail, PASSWORD)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.apartments.length()").value(1))
        .andExpect(jsonPath("$.apartments[0].unit_number").value("04"));
    assertEquals(1, count("resident_apartment_links", "id=? AND status='ENDED'", link));
  }

  @Test
  void adminCannotVerifyForeignApartmentOrForgeVerifier() throws Exception {
    send(
            base() + "/links",
            adminSession,
            Map.of(
                "userId",
                resident,
                "apartmentId",
                otherApartment,
                "verificationSource",
                "RESIDENT_LIST"))
        .andExpect(status().isNotFound());
    var data =
        new HashMap<String, Object>(
            Map.of(
                "userId",
                resident,
                "apartmentId",
                apartment,
                "verificationSource",
                "RESIDENT_LIST"));
    data.put("verifiedBy", technician);
    send(base() + "/links", adminSession, data).andExpect(status().isOk());
    assertEquals(
        1,
        count(
            "resident_apartment_links",
            "user_id=? AND apartment_id=? AND verified_by=?",
            resident,
            apartment,
            admin));
  }

  @Test
  void disablingAccountAlsoRevokesResetTokensBeforeReenable() throws Exception {
    send("/api/auth/forgot-password", null, Map.of("email", residentEmail))
        .andExpect(status().isOk());
    String token = tokenFor(residentEmail);
    send(
            base() + "/accounts/" + resident + "/disable",
            adminSession,
            Map.of("reason", "Khóa tài khoản"))
        .andExpect(status().isOk());
    send(
            base() + "/accounts/" + resident + "/enable",
            adminSession,
            Map.of("reason", "Cho phép nhận lời mời mới"))
        .andExpect(status().isOk());
    send("/api/auth/reset-password", null, Map.of("token", token, "password", NEW_PASSWORD))
        .andExpect(status().isGone());
    assertEquals(0, count("building_memberships", "user_id=? AND status='ACTIVE'", resident));
  }

  @Test
  void futureMembershipDoesNotGrantAccess() throws Exception {
    db.update(
        "UPDATE building_memberships SET valid_from=CURRENT_TIMESTAMP+INTERVAL '1 day' WHERE"
            + " user_id=?",
        resident);
    var session = login(residentEmail, PASSWORD);
    mvc.perform(get(base() + "/workspace/RESIDENT").session(session))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/auth/me").session(session))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.memberships.length()").value(0));
  }

  @Test
  void expiredInvitationCanBeReplacedWithoutDuplicateUser() throws Exception {
    UUID old = invite(invitedEmail);
    db.update(
        "UPDATE account_invitations SET expires_at=CURRENT_TIMESTAMP-INTERVAL '1 minute' WHERE"
            + " id=?",
        old);
    UUID latest = invite(invitedEmail);
    assertNotEquals(old, latest);
    assertEquals(1, count("users", "email=?", invitedEmail));
    assertEquals(1, count("account_invitations", "id=? AND status='EXPIRED'", old));
    accept(tokenFor(invitedEmail));
  }

  @Test
  void realEmailAddressesAreRejectedByDemo() throws Exception {
    send(
            base() + "/invitations",
            adminSession,
            inviteData("not-a-real-recipient@example.com", apartment))
        .andExpect(status().isUnprocessableContent());
    verifyNoInteractions(mail);
  }

  @Test
  void publicListingsNeverIncludeTokenHashes() throws Exception {
    invite(invitedEmail);
    mvc.perform(get(base() + "/invitations").session(adminSession))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].token_hash").doesNotExist());
  }
}
