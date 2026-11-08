import { useEffect, useRef, useState, useId } from "react";
import type { ReactNode } from "react";

export function Notice({
  children,
  error = false,
}: {
  children: ReactNode;
  error?: boolean;
}) {
  return (
    <p
      className={`notice ${error ? "error" : ""}`}
      role={error ? "alert" : "status"}
    >
      {children}
    </p>
  );
}
export function PasswordField({
  name = "password",
  label = "Mật khẩu",
  fresh = false,
}: {
  name?: string;
  label?: string;
  fresh?: boolean;
}) {
  const [show, setShow] = useState(false);
  return (
    <label className="field">
      {label}
      <span className="password-input">
        <input
          aria-label={label}
          name={name}
          type={show ? "text" : "password"}
          required
          minLength={fresh ? 15 : undefined}
          maxLength={fresh ? 256 : 512}
          autoComplete={fresh ? "new-password" : "current-password"}
        />
        <button
          type="button"
          className="plain"
          aria-label={show ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
          onClick={() => setShow(!show)}
        >
          {show ? "Ẩn" : "Hiện"}
        </button>
      </span>
    </label>
  );
}
export function Dialog({
  title,
  close,
  children,
}: {
  title: string;
  close: () => void;
  children: ReactNode;
}) {
  const ref = useRef<HTMLDialogElement>(null);
  const headingId = useId();
  useEffect(() => {
    ref.current?.showModal();
  }, []);
  return (
    <dialog ref={ref} onCancel={close} aria-labelledby={headingId}>
      <header className="dialog-heading">
        <h2 id={headingId}>{title}</h2>
        <button className="plain" onClick={close} aria-label="Đóng hộp thoại">
          Đóng
        </button>
      </header>
      {children}
    </dialog>
  );
}
export function ReasonDialog({
  title,
  description,
  close,
  confirm,
}: {
  title: string;
  description: string;
  close: () => void;
  confirm: (reason: string) => Promise<unknown>;
}) {
  const [error, setError] = useState(""),
    [busy, setBusy] = useState(false);
  return (
    <Dialog title={title} close={close}>
      <p>{description}</p>
      <form
        onSubmit={async (e) => {
          e.preventDefault();
          const reason = String(
            new FormData(e.currentTarget).get("reason"),
          ).trim();
          if (!reason) return;
          setBusy(true);
          setError("");
          try {
            await confirm(reason);
            close();
          } catch (e) {
            setError((e as Error).message);
          } finally {
            setBusy(false);
          }
        }}
      >
        <label className="field">
          Lý do
          <textarea name="reason" required maxLength={500} />
        </label>
        {error && <Notice error>{error}</Notice>}
        <div className="actions">
          <button type="button" className="secondary" onClick={close}>
            Hủy
          </button>
          <button disabled={busy}>{busy ? "Đang lưu…" : "Xác nhận"}</button>
        </div>
      </form>
    </Dialog>
  );
}
