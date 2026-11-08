import { useRemote } from "./hooks";
import { useState } from "react";
import type { Me } from "./api";
import { roleNames } from "./api";
import { Notice } from "./ui";

export default function Workspace({ me }: { me: Me }) {
  const [choice, setChoice] = useState(0);
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
      <div className="page-heading">
        <div>
          <p className="section-label">{m.building_name}</p>
          <h1>{roleNames[m.role]}</h1>
          <p>Xin chào {me.display_name}. Đây là không gian làm việc của bạn.</p>
        </div>
        {memberships.length > 1 && (
          <label className="field">
            Không gian làm việc
            <select
              value={choice}
              onChange={(e) => setChoice(Number(e.target.value))}
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
      <WorkspaceStatus building={m.building_id} role={m.role} />
      {m.role === "RESIDENT" && (
        <section className="account-section">
          <h2>Căn hộ của bạn</h2>
          {me.apartments.filter((a) => a.building_id === m.building_id)
            .length ? (
            <ul className="apartment-list">
              {me.apartments
                .filter((a) => a.building_id === m.building_id)
                .map((a) => (
                  <li key={a.id}>
                    <strong>Căn {a.unit_number}</strong>
                    <span>Đã được quản trị viên xác minh</span>
                  </li>
                ))}
            </ul>
          ) : (
            <p>
              Bạn chưa có liên kết căn hộ đang hiệu lực. Hãy liên hệ quản trị
              viên.
            </p>
          )}
        </section>
      )}
    </main>
  );
}
function WorkspaceStatus({
  building,
  role,
}: {
  building: string;
  role: string;
}) {
  const { data, error, busy, reload } = useRemote<{ message: string }>(
    `/api/buildings/${building}/workspace/${role}`,
  );
  return (
    <section className="account-section">
      <h2>Tài khoản sẵn sàng</h2>
      {busy ? (
        <Notice>Đang kiểm tra quyền…</Notice>
      ) : error ? (
        <>
          <Notice error>{error}</Notice>
          <button className="secondary" onClick={reload}>
            Kiểm tra lại
          </button>
        </>
      ) : (
        <p>{data?.message}</p>
      )}
    </section>
  );
}
