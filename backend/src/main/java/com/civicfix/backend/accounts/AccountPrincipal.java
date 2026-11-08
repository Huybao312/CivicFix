package com.civicfix.backend.accounts;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AccountPrincipal(UUID id, String email, String hash, long version, boolean enabled)
    implements UserDetails, Serializable {
  @Override
  public String toString() {
    return "AccountPrincipal[id=" + id + "]";
  }

  @Override
  public String getUsername() {
    return email;
  }

  @Override
  public String getPassword() {
    return hash;
  }

  @Override
  public boolean isEnabled() {
    return enabled;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return enabled;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }
}
