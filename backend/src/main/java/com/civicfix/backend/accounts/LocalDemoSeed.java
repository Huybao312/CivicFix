package com.civicfix.backend.accounts;

import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
public class LocalDemoSeed implements ApplicationRunner {
  public static final UUID BUILDING = UUID.fromString("00000000-0000-0000-0000-000000000001");
  public static final String PASSWORD = "CivicFix demo passphrase 2026!";
  final JdbcTemplate db;
  final PasswordEncoder encoder;

  public LocalDemoSeed(JdbcTemplate db, PasswordEncoder encoder) {
    this.db = db;
    this.encoder = encoder;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    db.update(
        "INSERT INTO buildings(id,name,address,time_zone) VALUES (?,?,'Địa chỉ giả"
            + " lập','Asia/Ho_Chi_Minh') ON CONFLICT(id) DO NOTHING",
        BUILDING,
        "Chung cư An Bình (demo)");
    for (int n = 3; n <= 8; n++)
      db.update(
          "INSERT INTO apartments(id,building_id,unit_number,floor) VALUES (?,?,?,'1') ON CONFLICT"
              + " DO NOTHING",
          new UUID(0, 100 + n),
          BUILDING,
          String.format("%02d", n));
    String[] roles = {"ADMIN", "MANAGER", "TECHNICIAN", "RESIDENT"};
    String[] names = {"Quản trị viên demo", "Quản lý demo", "Kỹ thuật viên demo", "Cư dân demo"};
    for (int n = 0; n < roles.length; n++) {
      UUID user = new UUID(0, 200 + n), membership = new UUID(0, 300 + n);
      String email = roles[n].toLowerCase() + "@demo.civicfix.local";
      db.update(
          "INSERT INTO users(id,email,display_name,password_hash,status,email_verified_at) VALUES"
              + " (?,?,?,?,'ACTIVE',CURRENT_TIMESTAMP) ON CONFLICT DO NOTHING",
          user,
          email,
          names[n],
          encoder.encode(PASSWORD));
      // Do not reactivate or reset existing demo accounts on restart.
      db.update(
          "INSERT INTO building_memberships(id,user_id,building_id,role,status) VALUES"
              + " (?,?,?,?,'ACTIVE') ON CONFLICT DO NOTHING",
          membership,
          user,
          BUILDING,
          roles[n]);
    }
    db.update(
        "INSERT INTO"
            + " resident_apartment_links(id,user_id,apartment_id,verified_by,verification_source)"
            + " VALUES (?,?,?,?,'RESIDENT_LIST') ON CONFLICT DO NOTHING",
        new UUID(0, 400),
        new UUID(0, 203),
        new UUID(0, 103),
        new UUID(0, 200));
  }
}
