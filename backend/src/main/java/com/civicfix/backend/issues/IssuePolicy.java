package com.civicfix.backend.issues;

import com.civicfix.backend.accounts.AccountAccess;
import com.civicfix.backend.accounts.ApiErrors.Problem;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class IssuePolicy {
  private final AccountAccess access;

  public IssuePolicy(AccountAccess access) {
    this.access = access;
  }

  public record Scope(boolean manager, boolean resident, boolean technician, UUID user) {
    public String sql(List<Object> args) {
      if (manager) return "TRUE";
      List<String> conditions = new ArrayList<>();
      if (resident) {
        conditions.add("i.reported_by=?");
        args.add(user);
      }
      if (technician) {
        conditions.add(
            "EXISTS(SELECT 1 FROM issue_assignments a WHERE a.issue_id=i.id AND a.technician_id=?"
                + " AND a.ended_at IS NULL)");
        args.add(user);
      }
      return conditions.isEmpty() ? "FALSE" : "(" + String.join(" OR ", conditions) + ")";
    }

    public Map<String, Object> project(Map<String, Object> row) {
      Map<String, Object> result = new LinkedHashMap<>(row);
      if (!manager && !(resident && user.equals(row.get("reported_by")))) {
        for (String field :
            List.of(
                "reported_by",
                "reporter_name_at_report",
                "apartment_id_at_report",
                "apartment_label_at_report")) {
          result.remove(field);
        }
      }
      return result;
    }
  }

  public Scope scope(UUID user, UUID building) {
    return new Scope(
        access.hasRole(user, building, "MANAGER"),
        access.hasRole(user, building, "RESIDENT"),
        access.hasRole(user, building, "TECHNICIAN"),
        user);
  }

  public Scope listScope(UUID user, UUID building) {
    Scope scope = scope(user, building);
    if (!scope.manager() && !scope.resident() && !scope.technician()) {
      throw new Problem(403, "Bạn không có quyền xem danh sách phiếu tại chung cư này.");
    }
    return scope;
  }
}
