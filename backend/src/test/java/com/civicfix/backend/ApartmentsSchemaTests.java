package com.civicfix.backend;

import java.util.UUID;

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
class ApartmentsSchemaTests {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void rejectsDuplicateUnitInSameBuilding() {
        UUID buildingId = createBuilding();
        createApartment(buildingId, "03");

        assertThrows(
            DuplicateKeyException.class,
            () -> createApartment(buildingId, "03")
        );
    }

    @Test
    void allowsSameUnitInDifferentBuildings() {
        UUID firstApartment = createApartment(createBuilding(), "03");
        UUID secondApartment = createApartment(createBuilding(), "03");

        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM apartments WHERE id IN (?, ?)",
            Long.class,
            firstApartment,
            secondApartment
        );

        assertEquals(Long.valueOf(2), count);
    }

    @Test
    void rejectsApartmentWithoutExistingBuilding() {
        UUID missingBuilding = UUID.randomUUID();

        assertThrows(
            DataIntegrityViolationException.class,
            () -> createApartment(missingBuilding, "03")
        );
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
            "Asia/Ho_Chi_Minh"
        );

        return id;
    }

    private UUID createApartment(UUID buildingId, String unitNumber) {
        UUID id = UUID.randomUUID();

        jdbcTemplate.update(
            """
            INSERT INTO apartments (id, building_id, unit_number)
            VALUES (?, ?, ?)
            """,
            id,
            buildingId,
            unitNumber
        );

        return id;
    }
}