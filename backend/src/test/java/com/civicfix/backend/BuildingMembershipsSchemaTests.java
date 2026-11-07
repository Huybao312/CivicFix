package com.civicfix.backend;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class BuildingMembershipsSchemaTests {

    @Autowired
    JdbcTemplate jdbcTemplate;

    private UUID userId;
    private UUID buildingId;

    @BeforeEach
    void prepareData() {
        userId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                        INSERT INTO users (
                            id, email, display_name, password_hash,
                            status, email_verified_at
                        )
                        VALUES (?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP)
                        """,
                userId,
                "resident@demo.civicfix.local",
                "Cư dân thử nghiệm",
                "schema-test-only");

        buildingId = createBuilding();
    }

    @Test
    void rejectsActiveRoleWhenSameRoleIsPending() {
        createMembership(buildingId, "RESIDENT", "PENDING");

        assertThrows(
                DuplicateKeyException.class,
                () -> createMembership(buildingId, "RESIDENT", "ACTIVE"));
    }

    @Test
    void allowsDifferentRolesInSameBuilding() {
        createMembership(buildingId, "RESIDENT", "ACTIVE");
        createMembership(buildingId, "MANAGER", "ACTIVE");

        assertEquals(Long.valueOf(2), membershipCount());
    }

    @Test
    void allowsSameRoleInDifferentBuildings() {
        createMembership(buildingId, "RESIDENT", "ACTIVE");
        createMembership(createBuilding(), "RESIDENT", "ACTIVE");

        assertEquals(Long.valueOf(2), membershipCount());
    }

    @Test
    void allowsRegrantAfterEndingMembershipAndKeepsHistory() {
        UUID oldMembership = createMembership(
                buildingId, "RESIDENT", "ACTIVE");

        jdbcTemplate.update(
                "UPDATE building_memberships SET status = 'ENDED' WHERE id = ?",
                oldMembership);

        createMembership(buildingId, "RESIDENT", "ACTIVE");

        assertEquals(Long.valueOf(2), membershipCount());

        String oldStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM building_memberships WHERE id = ?",
                String.class,
                oldMembership);

        assertEquals("ENDED", oldStatus);
    }

    private UUID createBuilding() {
        UUID id = UUID.randomUUID();

        jdbcTemplate.update(
                """
                        INSERT INTO buildings (id, name, address, time_zone)
                        VALUES (?, ?, ?, ?)
                        """,
                id,
                "Chung cư thử nghiệm",
                "Địa chỉ giả lập",
                "Asia/Ho_Chi_Minh");

        return id;
    }

    private UUID createMembership(
            UUID targetBuilding,
            String role,
            String status) {
        UUID id = UUID.randomUUID();

        jdbcTemplate.update(
                """
                        INSERT INTO building_memberships (
                            id, user_id, building_id, role, status
                        )
                        VALUES (?, ?, ?, ?, ?)
                        """,
                id,
                userId,
                targetBuilding,
                role,
                status);

        return id;
    }

    private Long membershipCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM building_memberships WHERE user_id = ?",
                Long.class,
                userId);
    }
}