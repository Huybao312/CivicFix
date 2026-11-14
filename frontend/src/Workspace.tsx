import { useState } from "react";
import type { Me } from "./api";
import { roleNames } from "./api";
import Issues from "./Issues";

export default function Workspace({ me }: { me: Me }) {
  const [choice, setChoice] = useState(() => {
    const params = new URLSearchParams(window.location.search);
    const available = me.memberships.filter((m) => m.role !== "ADMIN");
    return Math.max(
      0,
      available.findIndex(
        (m) =>
          m.building_id === params.get("building") &&
          m.role === params.get("role"),
      ),
    );
  });
  const memberships = me.memberships.filter((m) => m.role !== "ADMIN");
  const m = memberships[choice];
  if (!m)
    return (
      <main className="workspace">
        <p className="section-label">TÀI KHOẢN</p>
        <h1>Chưa có quyền đang hiệu lực</h1>
        <p>
          Bạn hiện không có membership đang hiệu lực. Liên hệ quản trị viên nếu
          đây là nhầm lẫn hoặc mở lời mời mới bạn đã nhận.
        </p>
      </main>
    );
  return (
    <main className="workspace">
      {window.location.pathname.startsWith("/issues/") ? (
        <p className="section-label">{m.building_name}</p>
      ) : (
        <div className="page-heading">
          <div>
            <p className="section-label">{m.building_name}</p>
            <h1>
              {m.role === "RESIDENT"
                ? "Phiếu của tôi"
                : m.role === "MANAGER"
                  ? "Phiếu chung cư"
                  : "Việc của tôi"}
            </h1>
            <p>Xin chào {me.display_name}.</p>
            {m.role === "RESIDENT" && (
              <p className="workspace-identity">
                {me.apartments.filter((a) => a.building_id === m.building_id)
                  .length
                  ? me.apartments
                      .filter((a) => a.building_id === m.building_id)
                      .map((a) => <span key={a.id}>Căn {a.unit_number}</span>)
                  : "Căn hộ chưa xác minh"}
              </p>
            )}
          </div>
          {memberships.length > 1 && (
            <label className="field">
              Không gian làm việc
              <select
                value={choice}
                onChange={(e) => {
                  window.history.replaceState({}, "", "/");
                  setChoice(Number(e.target.value));
                }}
              >
                {memberships.map((item, index) => (
                  <option key={item.id} value={index}>
                    {item.building_name} · {roleNames[item.role]}
                  </option>
                ))}
              </select>
            </label>
          )}
        </div>
      )}
      <Issues
        key={`${m.id}:${window.location.pathname}`}
        me={me}
        building={m.building_id}
        role={m.role}
      />
    </main>
  );
}
