package com.civicfix.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class BackendApplicationTests {

@Autowired
JdbcTemplate jdbcTemplate;

@Test
void migrationCreatesBuildingsTable() {
    String tableName = jdbcTemplate.queryForObject(
        "SELECT to_regclass('public.buildings')::text",
        String.class
    );

    assertEquals("buildings", tableName);
}

}
