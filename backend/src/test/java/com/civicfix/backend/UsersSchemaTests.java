package com.civicfix.backend;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class UsersSchemaTests {

    private static final String EMAIL = "resident@demo.civicfix.local";
    private static final OffsetDateTime VERIFIED_AT =
        OffsetDateTime.parse("2026-01-01T00:00:00Z");

    // Chỉ kiểm tra ràng buộc database; đây không phải hash dùng đăng nhập.
    private static final String HASH_FIXTURE = "schema-test-only";

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void allowsInvitedUserWithoutCredentials() {
        assertDoesNotThrow(
            () -> insertUser(EMAIL, null, "INVITED", null)
        );
    }

    @Test
    void allowsActiveUserWithCredentials() {
        assertDoesNotThrow(
            () -> insertUser(EMAIL, HASH_FIXTURE, "ACTIVE", VERIFIED_AT)
        );
    }

    @Test
    void rejectsDuplicateEmail() {
        insertUser(EMAIL, null, "INVITED", null);

        assertThrows(
            DuplicateKeyException.class,
            () -> insertUser(EMAIL, null, "INVITED", null)
        );
    }

    @Test
    void rejectsActiveUserWithoutPasswordHash() {
        assertThrows(
            DataIntegrityViolationException.class,
            () -> insertUser(EMAIL, null, "ACTIVE", VERIFIED_AT)
        );
    }

    @Test
    void rejectsActiveUserWithoutEmailVerification() {
        assertThrows(
            DataIntegrityViolationException.class,
            () -> insertUser(EMAIL, HASH_FIXTURE, "ACTIVE", null)
        );
    }

    @Test
    void rejectsEmailThatIsNotNormalized() {
        assertThrows(
            DataIntegrityViolationException.class,
            () -> insertUser(
                "Resident@demo.civicfix.local", null, "INVITED", null
            )
        );
    }

    @Test
    void rejectsUnknownStatus() {
        assertThrows(
            DataIntegrityViolationException.class,
            () -> insertUser(EMAIL, null, "UNKNOWN", null)
        );
    }

    private void insertUser(
        String email,
        String passwordHash,
        String status,
        OffsetDateTime verifiedAt
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO users (
                id, email, display_name, password_hash,
                status, email_verified_at
            )
            VALUES (?, ?, ?, ?, ?, ?)
            """,
            UUID.randomUUID(),
            email,
            "Cư dân thử nghiệm",
            passwordHash,
            status,
            verifiedAt
        );
    }
}