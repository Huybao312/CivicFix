import { useEffect, useState } from "react";
import { ApiError, request } from "./api";
import { Dialog, Notice } from "./ui";
import { FormField, ValidatedForm } from "./FormField";
import { useRemote } from "./hooks";

export type WorkflowIssue = {
  id: string;
  version: number;
  status: string;
  priority: string;
  question?: string | null;
  assignment?: {
    id: string;
    technician_id: string;
    technician_name: string;
  } | null;
  permissions?: { manage: boolean; provideInfo: boolean };
};
const priorities = [
  { value: "LOW", label: "Thấp" },
  { value: "MEDIUM", label: "Bình thường" },
  { value: "HIGH", label: "Cao" },
  { value: "URGENT", label: "Khẩn" },
];
type Action =
  | "ACCEPT"
  | "REQUEST_INFO"
  | "PROVIDE_INFO"
  | "REJECT"
  | "RESTORE"
  | "SET_PRIORITY"
  | "ASSIGN";
const titles: Record<Action, string> = {
  ACCEPT: "Tiếp nhận phiếu",
  REQUEST_INFO: "Yêu cầu bổ sung",
  PROVIDE_INFO: "Trả lời yêu cầu",
  REJECT: "Từ chối phiếu",
  RESTORE: "Khôi phục phiếu",
  SET_PRIORITY: "Đổi mức ưu tiên",
  ASSIGN: "Phân công kỹ thuật viên",
};
const allowed: Record<Action, string[]> = {
  ACCEPT: ["SUBMITTED"],
  REQUEST_INFO: ["SUBMITTED"],
  PROVIDE_INFO: ["NEEDS_INFO"],
  REJECT: ["SUBMITTED", "NEEDS_INFO", "ACCEPTED"],
  RESTORE: ["REJECTED"],
  SET_PRIORITY: [
    "SUBMITTED",
    "NEEDS_INFO",
    "ACCEPTED",
    "ASSIGNED",
    "IN_PROGRESS",
    "ON_HOLD",
  ],
  ASSIGN: ["ACCEPTED"],
};

export default function IssueActions({
  issue,
  base,
  updated,
  refresh,
}: {
  issue: WorkflowIssue;
  base: string;
  updated: (issue: WorkflowIssue) => void;
  refresh: () => Promise<WorkflowIssue>;
}) {
  const [action, setAction] = useState<Action | null>(null);
  const [notice, setNotice] = useState("");
  const [drafts, setDrafts] = useState<Partial<Record<Action, string>>>({});
  const choices: Action[] = issue.permissions?.manage
    ? (Object.keys(titles) as Action[]).filter(
        (a) => a !== "PROVIDE_INFO" && allowed[a].includes(issue.status),
      )
    : [];
  if (issue.permissions?.provideInfo && issue.status === "NEEDS_INFO")
    choices.push("PROVIDE_INFO");
  return (
    <>
      {issue.question && issue.status === "NEEDS_INFO" && (
        <aside className="question-panel">
          <h2>Thông tin cần bổ sung</h2>
          <p>{issue.question}</p>
        </aside>
      )}
      {notice && <Notice>{notice}</Notice>}
      {choices.length > 0 && (
        <section className="issue-command-panel" aria-label="Thao tác phiếu">
          <h2>
            {issue.permissions?.manage ? "Xử lý phiếu" : "Phản hồi của bạn"}
          </h2>
          <div className="issue-action-buttons">
            {choices.map((a) => (
              <button
                key={a}
                className={
                  a === "REJECT" ? "secondary danger-text" : "secondary"
                }
                onClick={() => {
                  setNotice("");
                  setAction(a);
                }}
              >
                {titles[a]}
              </button>
            ))}
          </div>
        </section>
      )}
      {action && (
        <ActionDialog
          key={action}
          action={action}
          issue={issue}
          base={base}
          draft={drafts[action] || ""}
          saveDraft={(text) =>
            setDrafts((previous) => ({ ...previous, [action]: text }))
          }
          close={() => setAction(null)}
          refresh={refresh}
          done={(result) => {
            updated(result);
            setDrafts((previous) => ({ ...previous, [action]: "" }));
            setAction(null);
            setNotice("Đã lưu thao tác vào lịch sử phiếu.");
          }}
        />
      )}
    </>
  );
}
function ActionDialog({
  action,
  issue,
  base,
  draft,
  saveDraft,
  close,
  refresh,
  done,
}: {
  action: Action;
  issue: WorkflowIssue;
  base: string;
  draft: string;
  saveDraft: (text: string) => void;
  close: () => void;
  refresh: () => Promise<WorkflowIssue>;
  done: (issue: WorkflowIssue) => void;
}) {
  const [text, setText] = useState(draft),
    [priority, setPriority] = useState(issue.priority || "MEDIUM"),
    [technician, setTechnician] = useState("");
  const [version, setVersion] = useState(issue.version),
    [key, setKey] = useState(() => crypto.randomUUID());
  const [packet, setPacket] = useState<Record<string, unknown> | null>(null);
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(""),
    [uncertain, setUncertain] = useState(false),
    [conflict, setConflict] = useState(false);
  const [technicianReady, setTechnicianReady] = useState(action !== "ASSIGN");
  const canApply = allowed[action].includes(issue.status);
  const textLabel =
    action === "REQUEST_INFO"
      ? "Câu hỏi cần cư dân bổ sung"
      : action === "PROVIDE_INFO"
        ? "Thông tin bổ sung"
        : action === "SET_PRIORITY"
          ? "Lý do nội bộ"
          : "Lý do công khai";
  async function submit() {
    const body = packet || {
      expectedVersion: version,
      text: text.trim(),
      ...(action === "SET_PRIORITY" ? { priority } : {}),
      ...(action === "ASSIGN" ? { technicianId: technician } : {}),
    };
    setBusy(true);
    setError("");
    setPacket(body);
    try {
      done(
        await request<WorkflowIssue>(`${base}/${issue.id}/actions/${action}`, {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Idempotency-Key": key,
          },
          body: JSON.stringify(body),
        }),
      );
    } catch (e) {
      setError((e as Error).message);
      if (e instanceof ApiError && e.status < 500) {
        setPacket(null);
        setUncertain(false);
        setConflict(e.status === 409);
      } else setUncertain(true);
    } finally {
      setBusy(false);
    }
  }
  async function reload() {
    setBusy(true);
    setError("");
    try {
      const latest = await refresh();
      setVersion(latest.version);
      setPacket(null);
      setKey(crypto.randomUUID());
      setConflict(false);
      if (uncertain) {
        close();
        return;
      }
      if (!allowed[action].includes(latest.status))
        setError(
          "Trạng thái phiếu đã đổi. Hãy xem lại các thao tác hiện có trên phiếu.",
        );
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Dialog title={titles[action]} close={close} busy={busy || uncertain}>
      <ValidatedForm
        label={titles[action]}
        onSubmit={(e) => {
          e.preventDefault();
          void submit();
        }}
      >
        {action === "ASSIGN" && (
          <TechnicianField
            base={base}
            value={technician}
            onChange={setTechnician}
            disabled={busy || uncertain || conflict}
            ready={setTechnicianReady}
          />
        )}
        {action === "SET_PRIORITY" && (
          <FormField
            label="Mức ưu tiên"
            name="priority"
            value={priority}
            onChange={setPriority}
            options={priorities}
            errorText="Chọn mức ưu tiên cho phiếu."
            disabled={busy || uncertain || conflict}
          />
        )}
        <FormField
          label={textLabel}
          name="text"
          value={text}
          onChange={(value) => {
            setText(value);
            saveDraft(value);
          }}
          hint={
            action === "SET_PRIORITY"
              ? "Ghi căn cứ điều chỉnh. Chỉ quản lý được xem lý do này."
              : "Người báo có thể xem nội dung này trong lịch sử phiếu."
          }
          errorText={
            action === "REQUEST_INFO"
              ? "Nhập câu hỏi cụ thể từ 3 đến 800 ký tự."
              : "Nhập nội dung từ 3 đến 800 ký tự."
          }
          autoFocus={action !== "ASSIGN"}
          disabled={busy || uncertain || conflict}
        />
        {error && <Notice error>{error}</Notice>}
        {conflict && (
          <Notice>
            Phiếu đã được cập nhật ở nơi khác. Tải lại để xem trạng thái mới
            trước khi xác nhận lại. Nội dung bạn nhập được giữ trong hộp thoại.
          </Notice>
        )}
        {uncertain && (
          <Notice>
            Chưa xác nhận được kết quả lưu. Gửi lại cùng nội dung hoặc xem lại
            phiếu và lịch sử.
          </Notice>
        )}
        <div className="actions">
          <button
            type="button"
            className="secondary"
            disabled={busy || uncertain}
            onClick={close}
          >
            Hủy
          </button>
          {(conflict || uncertain) && (
            <button
              type="button"
              className="secondary"
              disabled={busy}
              onClick={() => void reload()}
            >
              {uncertain ? "Xem phiếu đã cập nhật" : "Tải lại phiếu"}
            </button>
          )}
          <button
            type="submit"
            disabled={busy || conflict || !canApply || !technicianReady}
          >
            {busy
              ? "Đang lưu…"
              : uncertain
                ? "Gửi lại cùng nội dung"
                : titles[action]}
          </button>
        </div>
      </ValidatedForm>
    </Dialog>
  );
}
function TechnicianField({
  base,
  value,
  onChange,
  disabled,
  ready,
}: {
  base: string;
  value: string;
  onChange: (id: string) => void;
  disabled: boolean;
  ready: (ready: boolean) => void;
}) {
  const { data, busy, error, reload } = useRemote<
    { id: string; display_name: string }[]
  >(`${base}/technicians`);
  useEffect(() => {
    ready(!busy && !error && !!data?.length);
  }, [busy, error, data, ready]);
  return busy ? (
    <Notice>Đang tải kỹ thuật viên…</Notice>
  ) : error ? (
    <>
      <Notice error>{error}</Notice>
      <button type="button" onClick={reload}>
        Thử lại
      </button>
    </>
  ) : (
    <>
      <FormField
        label="Kỹ thuật viên"
        name="technicianId"
        value={value}
        onChange={onChange}
        options={(data || []).map((t) => ({
          value: t.id,
          label: t.display_name,
        }))}
        errorText="Chọn kỹ thuật viên đang có quyền tại chung cư."
        disabled={disabled}
        autoFocus
      />
      {!data?.length && (
        <Notice>Chưa có kỹ thuật viên đang hoạt động tại chung cư này.</Notice>
      )}
    </>
  );
}
