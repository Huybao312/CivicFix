package com.civicfix.backend;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class ResidentApartmentLinksSchemaTests {

    @Autowired
    JdbcTemplate jdbcTemplate;

    private UUID buildingId;
    private UUID residentId;
    private UUID verifierId;
    private UUID apartmentId;

    @BeforeEach
    void prepareData() {
        buildingId = UUID.randomUUID();

        jdbcTemplate.update(
                """
                        INSERT INTO buildings (id, name, address, time_zone)
                        VALUES (?, ?, ?, ?)
                        """,
                buildingId,
                "Chung cư thử nghiệm",
                "Địa chỉ giả lập",
                "Asia/Ho_Chi_Minh");

        residentId = createUser();
        verifierId = createUser();
        apartmentId = createApartment("03");
    }

    @Test
    void rejectsDuplicateVerifiedPair() {
        createLink(residentId, apartmentId);

        assertThrows(
                DuplicateKeyException.class,
                () -> createLink(residentId, apartmentId));
    }

    @Test
    void allowsMultipleResidentsInSameApartment() {
        createLink(residentId, apartmentId);
        createLink(createUser(), apartmentId);

        assertEquals(Long.valueOf(2), linkCount());
    }

    @Test
    void allowsResidentToHaveMultipleApartments() {
        createLink(residentId, apartmentId);
        createLink(residentId, createApartment("04"));

        assertEquals(Long.valueOf(2), linkCount());
    }

    @Test
    void allowsVerificationAgainAndKeepsEndedLink() {
        UUID oldLink = createLink(residentId, apartmentId);

        jdbcTemplate.update(
                """
                        UPDATE resident_apartment_links
                        SET status = 'ENDED', ended_at = CURRENT_TIMESTAMP
                        WHERE id = ?
                        """,
                oldLink);

        createLink(residentId, apartmentId);

        assertEquals(Long.valueOf(2), linkCount());

        String oldStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM resident_apartment_links WHERE id = ?",
                String.class,
                oldLink);

        assertEquals("ENDED", oldStatus);
    }

    @Test
    void rejectsEndedLinkWithoutEndTimestamp() {
        UUID linkId = createLink(residentId, apartmentId);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        """
                                UPDATE resident_apartment_links
                                SET status = 'ENDED'
                                WHERE id = ?
                                """,
                        linkId));
    }

    private UUID createUser() {
        UUID id = UUID.randomUUID();

        jdbcTemplate.update(
                """
                        INSERT INTO users (
                            id, email, display_name, password_hash,
                            status, email_verified_at
                        )
                        VALUES (?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP)
                        """,
                id,
                id + "@demo.civicfix.local",
                "Người dùng thử nghiệm",
                "schema-test-only");

        return id;
    }

    private UUID createApartment(String unitNumber) {
        UUID id = UUID.randomUUID();

        jdbcTemplate.update(
                """
                        INSERT INTO apartments (id, building_id, unit_number)
                        VALUES (?, ?, ?)
                        """,
                id,
                buildingId,
                unitNumber);

        return id;
    }

    private UUID createLink(UUID userId, UUID targetApartment) {
        UUID id = UUID.randomUUID();

        jdbcTemplate.update(
                """
                        INSERT INTO resident_apartment_links (
                            id, user_id, apartment_id,
                            verified_by, verification_source
                        )
                        VALUES (?, ?, ?, ?, ?)
                        """,
                id,
                userId,
                targetApartment,
                verifierId,
                "Đối chiếu danh sách cư dân giả lập");

        return id;
    }

    private Long linkCount() {
        return jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*) FROM resident_apartment_links
                        WHERE verified_by = ?
                        """,
                Long.class,
                verifierId);
    }
}