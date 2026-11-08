import { useRemote } from "./hooks";
import { useEffect, useState } from "react";
import type { Apartment, Me } from "./api";
import { date, post, roleNames, states } from "./api";
import { Dialog, Notice, ReasonDialog } from "./ui";

type Account = {
  id: string;
  membership_id: string;
  display_name: string;
  email: string;
  account_status: string;
  membership_status: string;
  role: string;
  apartments: string;
  effective: boolean;
  valid_from: string;
  valid_to?: string;
};
type Invitation = {
  id: string;
  display_name: string;
  email: string;
  unit_number: string;
  status: string;
  expires_at: string;
  last_sent_at: string;
  expired: boolean;
  incorrect_details: boolean;
  reason?: string;
};
type Audit = { id: string; action: string; reason: string; created_at: string };
type Link = {
  id: string;
  status: string;
  unit_number: string;
  verified_by: string;
  verified_at: string;
  verification_source: string;
};
type Action = { title: string; description: string; endpoint: string };
const sourceNames: Record<string, string> = {
  RESIDENT_LIST: "Đối chiếu danh sách cư dân",
  OPERATOR_CONFIRMATION: "Đơn vị vận hành xác nhận",
};
const auditNames: Record<string, string> = {
  INVITATION_CREATED: "Tạo lời mời",
  INVITATION_RESENT: "Gửi lại lời mời",
  INVITATION_REVOKED: "Thu hồi lời mời",
  INVITATION_ACCEPTED: "Chấp nhận lời mời",
  INVITATION_DETAILS_REPORTED: "Báo sai thông tin căn hộ",
  MEMBERSHIP_ENDED: "Kết thúc quyền",
  ACCOUNT_DISABLED: "Vô hiệu hóa tài khoản",
  ACCOUNT_ENABLED: "Cho phép kích hoạt lại tài khoản",
  APARTMENT_VERIFIED: "Xác minh căn hộ",
  APARTMENT_LINK_ENDED: "Kết thúc liên kết căn hộ",
};

export default function AdminPage({
  me,
  refresh,
}: {
  me: Me;
  refresh: () => Promise<void>;
}) {
  const buildings = me.memberships.filter((m) => m.role === "ADMIN");
  const [building, setBuilding] = useState(buildings[0].building_id);
  const [tab, setTab] = useState("accounts"),
    [filter, setFilter] = useState(""),
    [invite, setInvite] = useState(false);
  const [selected, setSelected] = useState<Account | null>(null),
    [action, setAction] = useState<Action | null>(null);
  const [notice, setNotice] = useState(""),
    [error, setError] = useState(""),
    [sending, setSending] = useState("");
  const [now, setNow] = useState(Date.now);
  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 5000);
    return () => window.clearInterval(timer);
  }, []);
  const base = `/api/buildings/${building}`;
  const accounts = useRemote<Account[]>(`${base}/accounts`),
    invitations = useRemote<Invitation[]>(`${base}/invitations`);
  const apartments = useRemote<Apartment[]>(`${base}/apartments`),
    audit = useRemote<Audit[]>(`${base}/audit`);
  const current =
    tab === "accounts" ? accounts : tab === "invitations" ? invitations : audit;
  function reload() {
    accounts.reload();
    invitations.reload();
    audit.reload();
    void refresh().catch((e: Error) => setError(e.message));
  }
  async function resend(id: string) {
    setSending(id);
    setError("");
    setNotice("");
    try {
      await post(`${base}/invitations/${id}/resend`);
      setNotice("Đã gửi lại lời mời. Liên kết cũ đã mất hiệu lực.");
      reload();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setSending("");
    }
  }
  const q = filter.toLocaleLowerCase("vi");
  return (
    <div className="admin-layout">
      <aside className="sidebar">
        <div className="workspace-name">
          <span className="section-label">QUẢN TRỊ TÀI KHOẢN</span>
          <strong>
            {buildings.find((m) => m.building_id === building)?.building_name}
          </strong>
        </div>
        <nav aria-label="Điều hướng quản trị">
          {[
            ["accounts", "Tài khoản cư dân"],
            ["invitations", "Lời mời"],
            ["audit", "Nhật ký thay đổi"],
          ].map(([key, label]) => (
            <button
              key={key}
              aria-current={tab === key ? "page" : undefined}
              onClick={() => {
                setTab(key);
                setFilter("");
                setError("");
                setNotice("");
              }}
            >
              {label}
            </button>
          ))}
        </nav>
        <div className="sidebar-foot">
          Thông tin căn hộ được xác minh bởi quản trị viên.
          {me.memberships.some((m) => m.role !== "ADMIN") && (
            <a href="/workspace">Mở vai trò khác →</a>
          )}
        </div>
      </aside>
      <main className="admin-content">
        <div className="page-heading">
          <div>
            <p className="section-label">{roleNames.ADMIN}</p>
            <h1>
              {tab === "accounts"
                ? "Tài khoản cư dân"
                : tab === "invitations"
                  ? "Lời mời tham gia"
                  : "Nhật ký thay đổi"}
            </h1>
            <p>
              {tab === "accounts"
                ? "Xác minh đúng người, đúng căn hộ và quản lý quyền truy cập."
                : tab === "invitations"
                  ? "Theo dõi lời mời, xử lý thông tin sai và thu hồi khi cần."
                  : "Các thao tác quan trọng được ghi lại để đối chiếu."}
            </p>
          </div>
          <button onClick={() => setInvite(true)}>Mời cư dân</button>
        </div>
        {buildings.length > 1 && (
          <label className="field building-select">
            Chung cư
            <select
              value={building}
              onChange={(e) => {
                setBuilding(e.target.value);
                setSelected(null);
              }}
            >
              {buildings.map((b) => (
                <option key={b.id} value={b.building_id}>
                  {b.building_name}
                </option>
              ))}
            </select>
          </label>
        )}
        {notice && <Notice>{notice}</Notice>}
        {error && <Notice error>{error}</Notice>}
        <div className="list-toolbar">
          <label className="search-field">
            <span>Tìm kiếm</span>
            <input
              type="search"
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              placeholder={
                tab === "audit"
                  ? "Tìm thao tác hoặc lý do"
                  : "Tên, email hoặc căn hộ"
              }
            />
          </label>
          <button className="secondary" onClick={current.reload}>
            Làm mới
          </button>
        </div>
        {current.busy && <Notice>Đang tải danh sách…</Notice>}
        {current.error && <Notice error>{current.error}</Notice>}
        {!current.busy && !current.error && tab === "accounts" && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Cư dân / nhân sự</th>
                  <th>Căn hộ</th>
                  <th>Vai trò</th>
                  <th>Quyền truy cập</th>
                  <th>
                    <span className="sr-only">Thao tác</span>
                  </th>
                </tr>
              </thead>
              <tbody>
                {accounts.data
                  ?.filter((a) =>
                    `${a.display_name} ${a.email} ${a.apartments}`
                      .toLocaleLowerCase("vi")
                      .includes(q),
                  )
                  .map((a) => (
                    <tr key={a.membership_id}>
                      <td data-label="Tài khoản">
                        <strong>{a.display_name}</strong>
                        <span className="subtext">{a.email}</span>
                      </td>
                      <td data-label="Căn hộ">
                        {a.apartments || "Chưa có liên kết"}
                      </td>
                      <td data-label="Vai trò">{roleNames[a.role]}</td>
                      <td data-label="Quyền">
                        <span
                          className={`status ${a.effective ? "active" : ""}`}
                        >
                          {a.account_status !== "ACTIVE"
                            ? states[a.account_status]
                            : a.membership_status === "ACTIVE" && !a.effective
                              ? new Date(a.valid_from).getTime() > now
                                ? "Chưa đến hiệu lực"
                                : "Hết thời hạn"
                              : states[a.membership_status]}
                        </span>
                      </td>
                      <td>
                        <button
                          className="text-link"
                          onClick={() => setSelected(a)}
                        >
                          Chi tiết<span className="sr-only"> {a.email}</span>
                        </button>
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
            {!accounts.data?.filter((a) =>
              `${a.display_name} ${a.email} ${a.apartments}`
                .toLowerCase()
                .includes(q),
            ).length && (
              <div className="empty-state">
                Không có tài khoản phù hợp. Bạn có thể điều chỉnh từ khóa hoặc
                mời cư dân mới.
              </div>
            )}
          </div>
        )}
        {!current.busy && !current.error && tab === "invitations" && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Người nhận</th>
                  <th>Căn hộ</th>
                  <th>Trạng thái</th>
                  <th>Hết hạn</th>
                  <th>Thao tác</th>
                </tr>
              </thead>
              <tbody>
                {invitations.data
                  ?.filter((i) =>
                    `${i.display_name} ${i.email} ${i.unit_number}`
                      .toLocaleLowerCase("vi")
                      .includes(q),
                  )
                  .map((i) => (
                    <tr key={i.id}>
                      <td data-label="Người nhận">
                        <strong>{i.display_name}</strong>
                        <span className="subtext">{i.email}</span>
                        {i.incorrect_details && (
                          <span className="review-note">
                            Cần kiểm tra lại thông tin căn hộ
                          </span>
                        )}
                      </td>
                      <td data-label="Căn hộ">Căn {i.unit_number}</td>
                      <td data-label="Trạng thái">
                        <span className="status">
                          {i.expired ? "Hết hạn" : states[i.status]}
                        </span>
                      </td>
                      <td data-label="Hết hạn">{date(i.expires_at)}</td>
                      <td className="row-actions">
                        {i.status === "PENDING" && (
                          <>
                            <button
                              className="text-link"
                              disabled={
                                sending === i.id ||
                                now - new Date(i.last_sent_at).getTime() < 60000
                              }
                              onClick={() => void resend(i.id)}
                            >
                              Gửi lại
                            </button>
                            <button
                              className="text-link danger-text"
                              onClick={() =>
                                setAction({
                                  title: "Thu hồi lời mời",
                                  description: `Liên kết gửi đến ${i.email} sẽ mất hiệu lực.`,
                                  endpoint: `${base}/invitations/${i.id}/revoke`,
                                })
                              }
                            >
                              Thu hồi
                            </button>
                          </>
                        )}
                        {i.reason && (
                          <span className="subtext">{i.reason}</span>
                        )}
                      </td>
                    </tr>
                  ))}
              </tbody>
            </table>
            {!invitations.data?.length && (
              <div className="empty-state">
                Chưa có lời mời. Chọn “Mời cư dân” để bắt đầu.
              </div>
            )}
            <p className="table-note">
              Có thể gửi lại sau ít nhất 60 giây. Mỗi lần gửi lại sẽ vô hiệu hóa
              liên kết cũ.
            </p>
          </div>
        )}
        {!current.busy && !current.error && tab === "audit" && (
          <ol className="audit-list">
            {audit.data
              ?.filter((a) =>
                `${auditNames[a.action] || a.action} ${a.reason || ""}`
                  .toLowerCase()
                  .includes(q),
              )
              .map((a) => (
                <li key={a.id}>
                  <div>
                    <strong>{auditNames[a.action] || a.action}</strong>
                    <p>{a.reason || "Không có ghi chú bổ sung."}</p>
                  </div>
                  <time>{date(a.created_at)}</time>
                </li>
              ))}
            {!audit.data?.length && <li>Chưa có thay đổi nào.</li>}
          </ol>
        )}
      </main>
      {invite && (
        <InviteDialog
          apartments={apartments.data || []}
          buildingName={
            buildings.find((b) => b.building_id === building)!.building_name
          }
          base={base}
          close={() => setInvite(false)}
          sent={() => {
            setInvite(false);
            setTab("invitations");
            setNotice("Lời mời đã được gửi vào hộp thư thử nghiệm.");
            reload();
          }}
        />
      )}
      {selected && (
        <AccountDialog
          me={me}
          account={selected}
          base={base}
          apartments={apartments.data || []}
          close={() => setSelected(null)}
          changed={() => {
            setSelected(null);
            reload();
            setNotice("Đã cập nhật thông tin và quyền truy cập.");
          }}
        />
      )}
      {action && (
        <ReasonDialog
          title={action.title}
          description={action.description}
          close={() => setAction(null)}
          confirm={async (reason) => {
            await post(action.endpoint, { reason });
            reload();
            setNotice("Đã thu hồi lời mời.");
          }}
        />
      )}
    </div>
  );
}

function InviteDialog({
  apartments,
  buildingName,
  base,
  close,
  sent,
}: {
  apartments: Apartment[];
  buildingName: string;
  base: string;
  close: () => void;
  sent: () => void;
}) {
  const [draft, setDraft] = useState<{
    email: string;
    displayName: string;
    apartmentId: string;
    verificationSource: string;
    validFrom?: string;
  } | null>(null);
  const [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  return (
    <Dialog
      title={draft ? "Kiểm tra trước khi gửi" : "Mời cư dân"}
      close={close}
    >
      <p>Chỉ mời sau khi đã xác minh thông tin cư dân ngoài ứng dụng.</p>
      {error && <Notice error>{error}</Notice>}
      {draft ? (
        <>
          <dl className="invitation-summary">
            <div>
              <dt>Người nhận</dt>
              <dd>
                {draft.displayName} · {draft.email}
              </dd>
            </div>
            <div>
              <dt>Chung cư / căn hộ</dt>
              <dd>
                {buildingName} · Căn{" "}
                {
                  apartments.find((a) => a.id === draft.apartmentId)
                    ?.unit_number
                }
              </dd>
            </div>
            <div>
              <dt>Căn cứ xác minh</dt>
              <dd>{sourceNames[draft.verificationSource]}</dd>
            </div>
            <div>
              <dt>Bắt đầu hiệu lực</dt>
              <dd>
                {draft.validFrom
                  ? date(draft.validFrom)
                  : "Sau khi người nhận chấp nhận lời mời"}
              </dd>
            </div>
          </dl>
          <p>
            Người nhận tự đặt mật khẩu hoặc đăng nhập bằng tài khoản hiện có.
            Lời mời có hạn 24 giờ.
          </p>
          <div className="actions">
            <button
              className="secondary"
              disabled={busy}
              onClick={() => setDraft(null)}
            >
              Sửa thông tin
            </button>
            <button
              disabled={busy}
              onClick={async () => {
                setBusy(true);
                setError("");
                try {
                  await post(`${base}/invitations`, draft);
                  sent();
                } catch (e) {
                  setError((e as Error).message);
                } finally {
                  setBusy(false);
                }
              }}
            >
              {busy ? "Đang gửi…" : "Xác nhận và gửi"}
            </button>
          </div>
        </>
      ) : (
        <form
          onSubmit={(e) => {
            e.preventDefault();
            const f = new FormData(e.currentTarget),
              validFrom = String(f.get("validFrom") || "");
            setDraft({
              email: String(f.get("email")).trim().toLowerCase(),
              displayName: String(f.get("displayName")).trim(),
              apartmentId: String(f.get("apartmentId")),
              verificationSource: String(f.get("verificationSource")),
              validFrom: validFrom
                ? new Date(validFrom).toISOString()
                : undefined,
            });
          }}
        >
          <label className="field">
            Tên cư dân
            <input
              name="displayName"
              required
              maxLength={160}
              autoComplete="name"
            />
          </label>
          <label className="field">
            Email nhận lời mời
            <input
              type="email"
              name="email"
              required
              maxLength={254}
              placeholder="cu.dan@demo.civicfix.local"
              autoComplete="email"
            />
          </label>
          <p className="helper">
            Bản demo chỉ nhận email @demo.civicfix.local.
          </p>
          <label className="field">
            Căn hộ đã xác minh
            <select name="apartmentId" required defaultValue="">
              <option value="" disabled>
                Chọn căn hộ
              </option>
              {apartments.map((a) => (
                <option key={a.id} value={a.id}>
                  Căn {a.unit_number}
                </option>
              ))}
            </select>
          </label>
          <SourceField />
          <label className="field">
            Bắt đầu hiệu lực (tùy chọn)
            <input type="datetime-local" name="validFrom" />
          </label>
          <label className="checkbox">
            <input type="checkbox" required />
            Tôi đã kiểm tra đúng người và đúng căn hộ.
          </label>
          <div className="actions">
            <button className="secondary" type="button" onClick={close}>
              Hủy
            </button>
            <button disabled={!apartments.length}>Kiểm tra lời mời</button>
          </div>
        </form>
      )}
    </Dialog>
  );
}
function SourceField() {
  return (
    <label className="field">
      Căn cứ xác minh
      <select name="verificationSource" required defaultValue="">
        <option value="" disabled>
          Chọn căn cứ
        </option>
        {Object.entries(sourceNames).map(([key, name]) => (
          <option key={key} value={key}>
            {name}
          </option>
        ))}
      </select>
    </label>
  );
}
function AccountDialog({
  me,
  account,
  base,
  apartments,
  close,
  changed,
}: {
  me: Me;
  account: Account;
  base: string;
  apartments: Apartment[];
  close: () => void;
  changed: () => void;
}) {
  const links = useRemote<Link[]>(`${base}/accounts/${account.id}/links`);
  const [action, setAction] = useState<Action | null>(null),
    [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  return (
    <>
      <Dialog title={account.display_name} close={close}>
        <p>{account.email}</p>
        <dl className="invitation-summary">
          <div>
            <dt>Vai trò</dt>
            <dd>{roleNames[account.role]}</dd>
          </div>
          <div>
            <dt>Tài khoản / quyền</dt>
            <dd>
              {states[account.account_status]} ·{" "}
              {states[account.membership_status]}
            </dd>
          </div>
        </dl>
        <h3>Liên kết căn hộ</h3>
        {links.busy && <Notice>Đang tải liên kết…</Notice>}
        {links.error && <Notice error>{links.error}</Notice>}
        {links.data?.map((l) => (
          <div className="link-row" key={l.id}>
            <div>
              <strong>
                Căn {l.unit_number} · {states[l.status]}
              </strong>
              <span className="subtext">
                {l.verified_by} · {date(l.verified_at)}
              </span>
            </div>
            {l.status === "VERIFIED" && (
              <button
                className="text-link danger-text"
                onClick={() =>
                  setAction({
                    title: "Kết thúc liên kết căn hộ",
                    description: `Cư dân sẽ không còn được chọn căn ${l.unit_number}. Lịch sử vẫn được giữ.`,
                    endpoint: `${base}/links/${l.id}/end`,
                  })
                }
              >
                Kết thúc
              </button>
            )}
          </div>
        ))}
        {!links.busy && !links.data?.length && <p>Chưa có liên kết căn hộ.</p>}
        {account.role === "RESIDENT" &&
          account.membership_status === "ACTIVE" &&
          account.account_status === "ACTIVE" && (
            <details className="link-form">
              <summary>Xác minh thêm căn hộ</summary>
              <form
                onSubmit={async (e) => {
                  e.preventDefault();
                  const f = new FormData(e.currentTarget);
                  setBusy(true);
                  setError("");
                  try {
                    await post(`${base}/links`, {
                      userId: account.id,
                      apartmentId: f.get("apartmentId"),
                      verificationSource: f.get("verificationSource"),
                    });
                    links.reload();
                  } catch (e) {
                    setError((e as Error).message);
                  } finally {
                    setBusy(false);
                  }
                }}
              >
                <label className="field">
                  Căn hộ
                  <select name="apartmentId" required defaultValue="">
                    <option value="" disabled>
                      Chọn căn hộ
                    </option>
                    {apartments.map((a) => (
                      <option key={a.id} value={a.id}>
                        Căn {a.unit_number}
                      </option>
                    ))}
                  </select>
                </label>
                <SourceField />
                <button disabled={busy}>
                  {busy ? "Đang lưu…" : "Lưu xác minh"}
                </button>
              </form>
            </details>
          )}
        {error && <Notice error>{error}</Notice>}
        {account.id !== me.id && (
          <div className="account-actions">
            {account.membership_status !== "ENDED" && (
              <button
                className="secondary danger-text"
                onClick={() =>
                  setAction({
                    title: "Kết thúc quyền tại chung cư",
                    description:
                      "Phiên đăng nhập cũ sẽ mất hiệu lực. Với vai trò cư dân, các liên kết căn hộ tại chung cư cũng kết thúc.",
                    endpoint: `${base}/memberships/${account.membership_id}/end`,
                  })
                }
              >
                Kết thúc quyền
              </button>
            )}
            {account.account_status === "DISABLED" && (
              <button
                className="secondary"
                onClick={() =>
                  setAction({
                    title: "Cho phép nhận lời mời trở lại",
                    description:
                      "Trạng thái tài khoản được khôi phục. Các quyền đã kết thúc sẽ không tự bật lại; hãy gửi lời mời mới nếu cần.",
                    endpoint: `${base}/accounts/${account.id}/enable`,
                  })
                }
              >
                Cho phép nhận lời mời lại
              </button>
            )}
            {account.account_status !== "DISABLED" && (
              <button
                className="secondary danger-text"
                onClick={() =>
                  setAction({
                    title: "Vô hiệu hóa tài khoản",
                    description:
                      "Tài khoản sẽ không đăng nhập được. Thao tác chỉ áp dụng khi không còn quyền tại chung cư khác.",
                    endpoint: `${base}/accounts/${account.id}/disable`,
                  })
                }
              >
                Vô hiệu hóa tài khoản
              </button>
            )}
          </div>
        )}
      </Dialog>
      {action && (
        <ReasonDialog
          title={action.title}
          description={action.description}
          close={() => setAction(null)}
          confirm={async (reason) => {
            await post(action.endpoint, { reason });
            setAction(null);
            changed();
          }}
        />
      )}
    </>
  );
}
