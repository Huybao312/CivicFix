import { useEffect, useRef, useState } from "react";
import type { Me } from "./api";
import { ApiError, date, request } from "./api";
import { useRemote } from "./hooks";
import { Notice } from "./ui";
import IssueActions from "./IssueActions";
import type { WorkflowIssue } from "./IssueActions";
import Notifications from "./Notifications";
import "./Issues.css";

type Issue = WorkflowIssue & {
  id: string;
  code: string;
  reported_by?: string;
  reporter_name_at_report?: string;
  apartment_label_at_report?: string | null;
  location_description: string;
  description: string;
  category: string;
  status: string;
  created_at: string;
};
type Event = {
  id: string;
  public_message: string;
  created_at: string;
  actor_name?: string;
};
const statusNames: Record<string, string> = {
  SUBMITTED: "Đã ghi nhận",
  NEEDS_INFO: "Cần thêm thông tin",
  ACCEPTED: "Đã tiếp nhận",
  ASSIGNED: "Đã phân công",
  IN_PROGRESS: "Đang xử lý",
  ON_HOLD: "Tạm hoãn",
  RESOLVED: "Đã khắc phục",
  CLOSED: "Đã đóng",
  REJECTED: "Không tiếp nhận",
};
const priorityNames: Record<string, string> = {
  LOW: "Thấp",
  MEDIUM: "Bình thường",
  HIGH: "Cao",
  URGENT: "Khẩn",
};
const categories: Record<string, string> = {
  OTHER: "Chưa rõ / khác",
  ELECTRICAL: "Điện / chiếu sáng",
  WATER: "Cấp thoát nước",
  ELEVATOR: "Thang máy",
};
function issueHref(building: string, role: string, id = "") {
  return `/issues${id ? "/" + id : ""}?building=${building}&role=${role}`;
}
export default function Issues({
  me,
  building,
  role,
}: {
  me: Me;
  building: string;
  role: string;
}) {
  const path = window.location.pathname;
  const base = `/api/buildings/${building}/issues`;
  const href = (id = "") => issueHref(building, role, id);
  if (path === "/issues/new" && role !== "TECHNICIAN")
    return <IssueForm me={me} building={building} base={base} href={href} />;
  const id = path.match(/^\/issues\/([^/]+)$/)?.[1];
  if (id)
    return <IssueDetail base={base} id={id} href={href} building={building} />;
  return (
    <>
      <Notifications building={building} href={href} />
      <IssueList
        base={base}
        manager={role === "MANAGER"}
        technician={role === "TECHNICIAN"}
        href={href}
      />
    </>
  );
}
function IssueList({
  base,
  manager,
  technician,
  href,
}: {
  base: string;
  manager: boolean;
  technician: boolean;
  href: (id?: string) => string;
}) {
  const [query, setQuery] = useState(""),
    [search, setSearch] = useState(""),
    [page, setPage] = useState(0),
    [status, setStatus] = useState(""),
    [priority, setPriority] = useState(""),
    [appliedStatus, setAppliedStatus] = useState(""),
    [appliedPriority, setAppliedPriority] = useState("");
  const { data, busy, error, reload } = useRemote<{
    items: Issue[];
    hasMore: boolean;
  }>(
    `${base}?page=${page}&size=20&q=${encodeURIComponent(search)}&status=${appliedStatus}&priority=${appliedPriority}`,
  );
  return (
    <section className="issue-workspace" aria-label="Danh sách phiếu">
      <div className="issue-toolbar">
        <p>
          {manager
            ? "Các báo cáo trong chung cư bạn quản lý."
            : technician
              ? "Các phiếu hiện được giao cho bạn."
              : "Các sự cố khu vực chung bạn đã báo."}
        </p>
        {!technician && (
          <a className="button-link" href={href("new")}>
            Báo sự cố khu vực chung
          </a>
        )}
      </div>
      <search>
        <form
          className="issue-search"
          method="get"
          aria-label="Tìm và lọc phiếu"
          onSubmit={(e) => {
            e.preventDefault();
            setSearch(query);
            setAppliedStatus(status);
            setAppliedPriority(priority);
            setPage(0);
          }}
        >
          <label className="field">
            Tìm phiếu
            <input
              type="search"
              name="q"
              autoCapitalize="none"
              autoCorrect="off"
              spellCheck={false}
              value={query}
              maxLength={120}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Mã phiếu, vị trí hoặc mô tả"
            />
          </label>
          <label className="field issue-filter">
            Trạng thái
            <select
              name="status"
              aria-label="Trạng thái"
              value={status}
              onChange={(e) => setStatus(e.target.value)}
            >
              <option value="">Tất cả</option>
              {Object.entries(statusNames).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <label className="field issue-filter">
            Ưu tiên
            <select
              name="priority"
              aria-label="Ưu tiên"
              value={priority}
              onChange={(e) => setPriority(e.target.value)}
            >
              <option value="">Tất cả</option>
              {Object.entries(priorityNames).map(([value, label]) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </label>
          <button type="submit" className="secondary">
            Tìm kiếm
          </button>
          <button type="button" className="secondary" onClick={reload}>
            Làm mới
          </button>
        </form>
      </search>
      {busy ? (
        <Notice>Đang tải danh sách phiếu…</Notice>
      ) : error ? (
        <>
          <Notice error>{error}</Notice>
          <button onClick={reload}>Thử lại</button>
        </>
      ) : data?.items.length ? (
        <>
          <div className="table-wrap issue-table">
            <table>
              <thead>
                <tr>
                  <th>Phiếu / vị trí</th>
                  {manager && <th>Người báo / căn hộ khi báo</th>}
                  <th>Trạng thái / ưu tiên</th>
                  <th>Ngày ghi nhận</th>
                </tr>
              </thead>
              <tbody>
                {data.items.map((i) => (
                  <tr key={i.id}>
                    <td>
                      <a className="issue-title" href={href(i.id)}>
                        <span className="issue-code">{i.code}</span>
                        <strong>{i.description}</strong>
                      </a>
                      <span className="subtext">{i.location_description}</span>
                    </td>
                    {manager && (
                      <td>
                        <strong>{i.reporter_name_at_report}</strong>
                        <span className="subtext">
                          {i.apartment_label_at_report
                            ? `Căn ${i.apartment_label_at_report}`
                            : "Căn hộ chưa xác minh"}
                        </span>
                      </td>
                    )}
                    <td>
                      <span className="status">
                        {statusNames[i.status] || i.status}
                      </span>
                      <span
                        className={`subtext priority-${i.priority?.toLowerCase()}`}
                      >
                        {priorityNames[i.priority] || "Bình thường"}
                      </span>
                    </td>
                    <td>{date(i.created_at)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <nav className="issue-pagination" aria-label="Phân trang phiếu">
            <button
              className="secondary"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
            >
              Trang trước
            </button>
            <span>Trang {page + 1}</span>
            <button
              className="secondary"
              disabled={!data.hasMore}
              onClick={() => setPage((p) => p + 1)}
            >
              Trang sau
            </button>
          </nav>
        </>
      ) : (
        <div className="issue-empty">
          <h2>
            {search || appliedStatus || appliedPriority
              ? "Không tìm thấy phiếu phù hợp"
              : technician
                ? "Chưa có việc được giao"
                : "Chưa có phiếu nào"}
          </h2>
          <p>
            {search || appliedStatus || appliedPriority
              ? "Thử từ khóa hoặc bộ lọc khác."
              : technician
                ? "Phiếu sẽ xuất hiện khi quản lý phân công cho bạn."
                : "Khi có sự cố ở khu vực chung, bạn có thể gửi báo cáo tại đây."}
          </p>
        </div>
      )}
      <p className="table-note">
        “Đã ghi nhận” nghĩa là báo cáo đã được lưu, chưa đồng nghĩa đã có người
        đọc hoặc đang sửa.
      </p>
    </section>
  );
}
function EmergencyNote() {
  return (
    <aside className="emergency-note">
      <strong>Có nguy hiểm tức thời?</strong>
      <p>
        Liên hệ trực tiếp đầu mối khẩn cấp do chung cư công bố hoặc lực lượng
        ứng cứu phù hợp. Không chờ phản hồi qua phiếu web. Bản demo chưa có số
        liên hệ được xác nhận.
      </p>
    </aside>
  );
}
function IssueForm({
  me,
  building,
  base,
  href,
}: {
  me: Me;
  building: string;
  base: string;
  href: (id?: string) => string;
}) {
  const apartments = me.apartments.filter((a) => a.building_id === building);
  const [scope, setScope] = useState("COMMON_AREA"),
    [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [key] = useState(() => crypto.randomUUID());
  const [packet, setPacket] = useState<Record<string, unknown> | null>(null);
  const [uncertain, setUncertain] = useState(false);
  async function submit(form: HTMLFormElement) {
    const fields = new FormData(form);
    const body = packet || {
      scope,
      locationDescription: String(fields.get("locationDescription")).trim(),
      description: String(fields.get("description")).trim(),
      category: fields.get("category"),
      apartmentLinkId: fields.get("apartmentLinkId") || null,
    };
    setPacket(body);
    setBusy(true);
    setError("");
    try {
      const issue = await request<Issue>(base, {
        method: "POST",
        headers: { "Content-Type": "application/json", "Idempotency-Key": key },
        body: JSON.stringify(body),
      });
      window.location.assign(href(issue.id) + "&submitted=1");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Không thể gửi báo cáo.");
      // A network/server failure may have committed; retain the same payload and key for retry.
      if (e instanceof ApiError && e.status >= 400 && e.status < 500) {
        setPacket(null);
        setUncertain(false);
      } else setUncertain(true);
    } finally {
      setBusy(false);
    }
  }
  return (
    <section className="issue-form-page">
      <a className="back-link" href={href()}>
        ← Danh sách phiếu
      </a>
      <h1>Báo sự cố khu vực chung</h1>
      <p>Chỉ cần vị trí cụ thể và mô tả điều bạn quan sát được.</p>
      <EmergencyNote />
      <form
        onSubmit={(e) => {
          e.preventDefault();
          void submit(e.currentTarget);
        }}
      >
        <fieldset disabled={busy || uncertain}>
          <legend className="visually-hidden">Thông tin báo cáo</legend>
          <label className="field">
            Phạm vi sự cố
            <select value={scope} onChange={(e) => setScope(e.target.value)}>
              <option value="COMMON_AREA">Khu vực chung của chung cư</option>
              <option value="PRIVATE_APARTMENT">
                Thiết bị riêng bên trong căn hộ
              </option>
            </select>
          </label>
          {scope === "PRIVATE_APARTMENT" ? (
            <Notice error>
              CivicFix hiện chỉ tiếp nhận khu vực chung. Hãy liên hệ đơn vị phụ
              trách thiết bị riêng trong căn hộ qua kênh chung cư đã công bố.
            </Notice>
          ) : (
            <>
              <label className="field">
                Vị trí xảy ra sự cố
                <input
                  name="locationDescription"
                  required
                  minLength={3}
                  maxLength={300}
                  placeholder="Ví dụ: Hành lang tầng 3, gần thang máy B"
                />
              </label>
              <label className="field">
                Mô tả sự cố
                <textarea
                  name="description"
                  required
                  minLength={10}
                  maxLength={3000}
                  rows={4}
                  placeholder="Ví dụ: Đèn chớp liên tục vào buổi tối, hành lang khá tối."
                />
              </label>
              <label className="field">
                Loại sự cố · không bắt buộc
                <select name="category" defaultValue="OTHER">
                  {Object.entries(categories).map(([value, label]) => (
                    <option key={value} value={value}>
                      {label}
                    </option>
                  ))}
                </select>
              </label>
              {apartments.length > 1 ? (
                <label className="field">
                  Căn hộ của bạn khi báo
                  <select name="apartmentLinkId" required defaultValue="">
                    <option value="" disabled>
                      Chọn căn hộ đã xác minh
                    </option>
                    {apartments.map((a) => (
                      <option key={a.id} value={a.link_id}>
                        Căn {a.unit_number}
                      </option>
                    ))}
                  </select>
                </label>
              ) : (
                <p className="reporter-note">
                  <strong>Người báo: {me.display_name}</strong>
                  <br />
                  {apartments.length
                    ? `Căn ${apartments[0].unit_number} đã xác minh`
                    : "Căn hộ chưa xác minh"}
                  <br />
                  <span>Căn hộ người báo khác với vị trí xảy ra sự cố.</span>
                </p>
              )}
              <p className="helper attachment-note">
                Hiện chưa hỗ trợ đính kèm ảnh. Hãy mô tả rõ dấu hiệu bạn quan
                sát được.
              </p>
            </>
          )}
        </fieldset>
        {error && <Notice error>{error}</Notice>}
        {uncertain && (
          <Notice>
            Chưa xác nhận được kết quả gửi. Bạn có thể gửi lại cùng nội dung; hệ
            thống dùng cùng mã yêu cầu để tránh tạo trùng. Hoặc kiểm tra danh
            sách phiếu trước khi báo mới.
          </Notice>
        )}
        <div className="actions">
          <a className="back-link" href={href()}>
            Về danh sách
          </a>
          <button disabled={busy || scope !== "COMMON_AREA"}>
            {busy
              ? "Đang gửi…"
              : uncertain
                ? "Gửi lại cùng nội dung"
                : "Gửi báo cáo"}
          </button>
        </div>
      </form>
    </section>
  );
}
function IssueDetail({
  base,
  id,
  href,
  building,
}: {
  base: string;
  id: string;
  href: (id?: string) => string;
  building: string;
}) {
  const [updated, setUpdated] = useState<Issue | null>(null);
  const heading = useRef<HTMLHeadingElement>(null);
  const [notificationError, setNotificationError] = useState("");
  const issue = useRemote<Issue>(`${base}/${encodeURIComponent(id)}`),
    history = useRemote<Event[]>(`${base}/${encodeURIComponent(id)}/events`);
  const notification = new URLSearchParams(window.location.search).get(
    "notification",
  );
  useEffect(() => {
    if (!issue.data || !notification) return;
    void request(
      `/api/buildings/${building}/notifications/${encodeURIComponent(notification)}/read`,
      { method: "POST" },
    ).catch((e: Error) => setNotificationError(e.message));
  }, [issue.data, notification, building]);
  if (issue.busy) return <Notice>Đang tải phiếu…</Notice>;
  if (issue.error)
    return (
      <>
        <a href={href()}>← Danh sách phiếu</a>
        <Notice error>{issue.error}</Notice>
        <button onClick={issue.reload}>Thử lại</button>
      </>
    );
  const i =
    updated && updated.version >= issue.data!.version ? updated : issue.data!;
  function applied(next: WorkflowIssue) {
    setUpdated(next as Issue);
    history.reload();
  }
  async function refresh() {
    const next = await request<Issue>(`${base}/${encodeURIComponent(id)}`);
    applied(next);
    return next;
  }
  return (
    <article className="issue-detail">
      <a className="back-link" href={href()}>
        ← Danh sách phiếu
      </a>
      {notificationError && <Notice error>{notificationError}</Notice>}
      {new URLSearchParams(window.location.search).has("submitted") &&
        i.status === "SUBMITTED" && (
          <Notice>
            Báo cáo {i.code} đã được ghi nhận. Quản lý sẽ xem xét trước khi tiếp
            nhận xử lý.
          </Notice>
        )}
      <div className="issue-detail-heading">
        <h1 ref={heading} tabIndex={-1}>
          {i.code}
        </h1>
        <span className="status">{statusNames[i.status] || i.status}</span>
      </div>
      <dl className="issue-facts">
        <div>
          <dt>Vị trí xảy ra sự cố</dt>
          <dd>{i.location_description}</dd>
        </div>
        {i.reporter_name_at_report !== undefined && (
          <>
            <div>
              <dt>Người báo</dt>
              <dd>{i.reporter_name_at_report}</dd>
            </div>
            <div>
              <dt>Căn hộ khi báo</dt>
              <dd>
                {i.apartment_label_at_report
                  ? `Căn ${i.apartment_label_at_report} · đã xác minh khi báo`
                  : "Căn hộ chưa xác minh khi báo"}
              </dd>
            </div>
          </>
        )}
        <div>
          <dt>Ngày ghi nhận</dt>
          <dd>{date(i.created_at)}</dd>
        </div>
        <div>
          <dt>Loại sự cố</dt>
          <dd>{categories[i.category] || i.category}</dd>
        </div>
        <div>
          <dt>Ưu tiên</dt>
          <dd>{priorityNames[i.priority] || "Bình thường"}</dd>
        </div>
        {i.assignment && (
          <div>
            <dt>Kỹ thuật viên phụ trách</dt>
            <dd>{i.assignment.technician_name}</dd>
          </div>
        )}
      </dl>
      <IssueActions
        issue={i}
        base={base}
        updated={(next) => {
          applied(next);
          requestAnimationFrame(() => heading.current?.focus());
        }}
        refresh={refresh}
      />
      <section className="issue-description">
        <h3>Mô tả sự cố</h3>
        <p>{i.description}</p>
      </section>
      <section>
        <h3>Lịch sử phiếu</h3>
        {history.busy ? (
          <Notice>Đang tải lịch sử…</Notice>
        ) : history.error ? (
          <>
            <Notice error>{history.error}</Notice>
            <button onClick={history.reload}>Tải lại lịch sử</button>
          </>
        ) : (
          <ol className="issue-history">
            {history.data?.map((e) => (
              <li key={e.id}>
                <time>{date(e.created_at)}</time>
                {e.actor_name && (
                  <span className="history-actor"> · {e.actor_name}</span>
                )}
                <p>{e.public_message}</p>
              </li>
            ))}
          </ol>
        )}
      </section>
    </article>
  );
}
