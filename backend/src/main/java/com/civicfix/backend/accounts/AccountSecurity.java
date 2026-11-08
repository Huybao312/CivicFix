package com.civicfix.backend.accounts;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class AccountSecurity {
  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
  }

  @Bean
  UserDetailsService users(JdbcTemplate db, PasswordEncoder encoder) {
    String dummyHash = encoder.encode("unavailable account dummy password");
    return email ->
        db
            .query(
                """
                SELECT id,email,password_hash,auth_version,status,email_verified_at FROM users WHERE email=?
                """,
                (rs, n) ->
                    new AccountPrincipal(
                        rs.getObject("id", java.util.UUID.class),
                        rs.getString("email"),
                        (rs.getString("password_hash") == null
                            ? dummyHash
                            : rs.getString("password_hash")),
                        rs.getLong("auth_version"),
                        "ACTIVE".equals(rs.getString("status"))
                            && rs.getTimestamp("email_verified_at") != null),
                email.strip().toLowerCase(Locale.ROOT))
            .stream()
            .findFirst()
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
  }

  @Bean
  SecurityFilterChain security(
      HttpSecurity http,
      UserDetailsService users,
      PasswordEncoder encoder,
      JdbcTemplate db,
      RequestLimits limits,
      AccountAccess access)
      throws Exception {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(users);
    provider.setPasswordEncoder(encoder);
    provider.setPreAuthenticationChecks(user -> {});
    provider.setPostAuthenticationChecks(user -> {
      if (!user.isEnabled())
        throw new org.springframework.security.authentication.BadCredentialsException(
            "Invalid credentials");
    });
    http.authenticationProvider(provider)
        .authorizeHttpRequests(a ->
            a.requestMatchers(
                    "/api/auth/csrf",
                    "/api/auth/login",
                    "/api/auth/forgot-password",
                    "/api/auth/reset-password",
                    "/api/invitations/preview",
                    "/api/invitations/accept",
                    "/api/invitations/report-incorrect",
                    "/error")
                .permitAll()
                .anyRequest()
                .authenticated())
        .requestCache(c -> c.requestCache(new NullRequestCache()))
        .httpBasic(AbstractHttpConfigurer::disable)
        .formLogin(f ->
            f.loginPage("/login")
                .loginProcessingUrl("/api/auth/login")
                .usernameParameter("email")
                .successHandler((req, res, auth) -> {
                  var p = (AccountPrincipal) auth.getPrincipal();
                  access.audit(p.id(), null, "LOGIN", p.id(), null);
                  json(res, 200, "{\"ok\":true}");
                })
                .failureHandler((req, res, e) -> {
                  access.audit(null, null, "LOGIN_FAILED", null, "Invalid credentials");
                  json(
                      res,
                      401,
                      "{\"message\":\"Không thể đăng nhập với thông tin này. Kiểm tra lại hoặc đặt"
                          + " lại mật khẩu.\"}");
                })
                .permitAll())
        .logout(l ->
            l.logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((req, res, auth) -> json(res, 200, "{\"ok\":true}")))
        .exceptionHandling(e ->
            e.authenticationEntryPoint(
                    (req, res, err) -> json(res, 401, "{\"message\":\"Vui lòng đăng nhập.\"}"))
                .accessDeniedHandler((req, res, err) ->
                    json(
                        res,
                        403,
                        "{\"message\":\"Phiên xác nhận không hợp lệ hoặc bạn không có quyền. Hãy"
                            + " tải lại trang.\"}")))
        .addFilterAfter(new FreshAccountFilter(db), SecurityContextHolderFilter.class)
        .addFilterBefore(new LoginLimitFilter(limits), CsrfFilter.class);
    return http.build();
  }

  static void json(HttpServletResponse response, int status, String body) throws IOException {
    response.setStatus(status);
    response.setContentType("application/json;charset=UTF-8");
    response.setHeader("Cache-Control", "no-store");
    response.getWriter().write(body);
  }

  static class LoginLimitFilter extends OncePerRequestFilter {
    final RequestLimits limits;

    LoginLimitFilter(RequestLimits limits) {
      this.limits = limits;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws IOException, ServletException {
      if ("POST".equals(req.getMethod()) && "/api/auth/login".equals(req.getRequestURI())) {
        String email = req.getParameter("email");
        if (email == null
            || email.length() > 254
            || !limits.take(
                "login:" + req.getRemoteAddr() + ":" + email.strip().toLowerCase(Locale.ROOT),
                15,
                900)
            || !limits.take("login-ip:" + req.getRemoteAddr(), 100, 900)) {
          res.setHeader("Retry-After", "900");
          json(res, 429, "{\"message\":\"Bạn đã thử nhiều lần. Vui lòng thử lại sau 15 phút.\"}");
          return;
        }
      }
      if (req.getRequestURI().startsWith("/api/")) res.setHeader("Cache-Control", "no-store");
      chain.doFilter(req, res);
    }
  }

  static class FreshAccountFilter extends OncePerRequestFilter {
    final JdbcTemplate db;

    FreshAccountFilter(JdbcTemplate db) {
      this.db = db;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws IOException, ServletException {
      var auth = SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.getPrincipal() instanceof AccountPrincipal p) {
        List<Long> versions =
            db.query(
                "SELECT auth_version FROM users WHERE id=? AND status='ACTIVE'",
                (rs, n) -> rs.getLong(1),
                p.id());
        if (versions.isEmpty() || versions.getFirst() != p.version()) {
          if (req.getSession(false) != null) req.getSession(false).invalidate();
          SecurityContextHolder.clearContext();
          json(res, 401, "{\"message\":\"Phiên đăng nhập đã kết thúc. Vui lòng đăng nhập lại.\"}");
          return;
        }
      }
      chain.doFilter(req, res);
    }
  }
}
