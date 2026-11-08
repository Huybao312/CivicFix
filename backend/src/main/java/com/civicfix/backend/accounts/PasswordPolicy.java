package com.civicfix.backend.accounts;

import static com.civicfix.backend.accounts.ApiErrors.Problem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicy {
  private final Set<String> common;

  public PasswordPolicy() throws IOException {
    try (var in = new ClassPathResource("common-passwords.txt").getInputStream()) {
      common =
          new String(in.readAllBytes(), StandardCharsets.UTF_8)
              .lines()
              .map(s -> s.toLowerCase(Locale.ROOT))
              .collect(Collectors.toUnmodifiableSet());
    }
  }

  public void check(String password) {
    if (password == null
        || password.codePointCount(0, password.length()) < 15
        || password.codePointCount(0, password.length()) > 128
        || password.codePoints().distinct().count() < 3
        || common.contains(password.toLowerCase(Locale.ROOT)))
      throw new Problem(
          422, "Dùng mật khẩu từ 15 đến 128 ký tự, tránh mật khẩu phổ biến hoặc lặp lại.");
  }
}
