import { useEffect, useState } from "react";
import { ApiError, login, post } from "./api";
import type { Me } from "./api";
import { Notice, PasswordField } from "./ui";

type Props = {
  path: string;
  token: string;
  me: Me | null;
  navigate: (path: string) => void;
  refresh: () => Promise<void>;
};
type Preview = {
  building_name: string;
  unit_number: string;
  email: string;
  existing_account: boolean;
};

export default function AuthPages({
  path,
  token,
  me,
  navigate,
  refresh,
}: Props) {
  const [busy, setBusy] = useState(false),
    [error, setError] = useState(
      path === "/activate" && !token
        ? "Liên kết không còn dùng được. Hãy liên hệ quản trị viên để nhận lời mời mới."
        : "",
    ),
    [done, setDone] = useState("");
  const [preview, setPreview] = useState<Preview | null>(null);
  const [previewBusy, setPreviewBusy] = useState(
    path === "/activate" && !!token,
  );
  useEffect(() => {
    let active = true;
    if (path !== "/activate") return;
    if (!token) return;
    post<Preview>("/api/invitations/preview", { token })
      .then((p) => {
        if (active) setPreview(p);
      })
      .catch((e) => {
        if (active) setError(e.message);
      })
      .finally(() => {
        if (active) setPreviewBusy(false);
      });
    return () => {
      active = false;
    };
  }, [path, token]);
  async function perform(action: () => Promise<void>) {
    setBusy(true);
    setError("");
    try {
      await action();
    } catch (e) {
      setError(
        e instanceof ApiError
          ? e.message
          : "Không kết nối được. Kiểm tra mạng và thử lại.",
      );
    } finally {
      setBusy(false);
    }
  }
  const forgot = path === "/forgot-password",
    reset = path === "/reset-password",
    activate = path === "/activate";
  const title = forgot
    ? "Quên mật khẩu"
    : reset
      ? "Đặt mật khẩu mới"
      : activate
        ? "Xác nhận lời mời"
        : "Đăng nhập";
  return (
    <main className="auth-page">
      <section className="auth-intro">
        <p className="section-label">CỔNG CƯ DÂN</p>
        <h1>{title}</h1>
        <p>
          {activate
            ? "Kiểm tra thông tin trước khi chấp nhận."
            : "Theo dõi sự cố, nhận cập nhật và liên hệ đúng người phụ trách."}
        </p>
        <div className="auth-note">
          <strong>Một tài khoản, đúng căn hộ.</strong>
          <p>
            Quản trị viên xác minh thông tin và gửi lời mời. Bạn tự đặt mật khẩu
            của mình.
          </p>
        </div>
      </section>
      <section className="auth-form-panel" aria-label={title}>
        {previewBusy ? (
          <Notice>Đang kiểm tra lời mời…</Notice>
        ) : done ? (
          <>
            <h2>{done}</h2>
            <p>
              {forgot
                ? "Kiểm tra hộp thư và mục thư rác nếu tài khoản đủ điều kiện."
                : activate
                  ? done.startsWith("Đã báo")
                    ? "Việc kích hoạt đã được dừng. Hãy chờ quản trị viên kiểm tra thông tin."
                    : "Bạn có thể đăng nhập để xem tài khoản và căn hộ của mình."
                  : "Các phiên đăng nhập cũ đã được thu hồi."}
            </p>
            <button onClick={() => navigate("/login")}>
              Về trang đăng nhập
            </button>
          </>
        ) : (
          <>
            {error && <Notice error>{error}</Notice>}
            {activate && preview && (
              <dl className="invitation-summary">
                <div>
                  <dt>Chung cư</dt>
                  <dd>{preview.building_name}</dd>
                </div>
                <div>
                  <dt>Căn hộ được xác minh</dt>
                  <dd>Căn {preview.unit_number}</dd>
                </div>
                <div>
                  <dt>Email nhận lời mời</dt>
                  <dd>{preview.email}</dd>
                </div>
              </dl>
            )}
            {activate && !preview ? (
              <button className="secondary" onClick={() => navigate("/login")}>
                Về trang đăng nhập
              </button>
            ) : (
              <form
                onSubmit={(e) => {
                  e.preventDefault();
                  const form = new FormData(e.currentTarget);
                  void perform(async () => {
                    const password = String(form.get("password") || "");
                    if (
                      (reset || (activate && !preview?.existing_account)) &&
                      password !== form.get("confirm")
                    )
                      throw new ApiError(422, "Hai mật khẩu chưa khớp nhau.");
                    if (forgot) {
                      const response = await post<{ message: string }>(
                        "/api/auth/forgot-password",
                        { email: form.get("email") },
                      );
                      setDone(response.message);
                    } else if (reset) {
                      if (!token)
                        throw new ApiError(
                          410,
                          "Liên kết không còn dùng được. Hãy yêu cầu đặt lại mật khẩu.",
                        );
                      await post("/api/auth/reset-password", {
                        token,
                        password,
                      });
                      await refresh();
                      setDone("Mật khẩu đã được đổi");
                    } else if (activate) {
                      if (
                        preview?.existing_account &&
                        (!me || me.email !== preview.email)
                      ) {
                        await login(preview!.email, password);
                      }
                      await post("/api/invitations/accept", {
                        token,
                        password: preview?.existing_account
                          ? undefined
                          : password,
                      });
                      await refresh();
                      setDone("Lời mời đã được chấp nhận");
                    } else {
                      await login(String(form.get("email")), password);
                      await refresh();
                      navigate("/");
                    }
                  });
                }}
              >
                {!reset && !activate && (
                  <label className="field">
                    Email
                    <input
                      type="email"
                      name="email"
                      autoComplete="username"
                      required
                      maxLength={254}
                      placeholder="ten@demo.civicfix.local"
                    />
                  </label>
                )}
                {!forgot &&
                  !(
                    activate &&
                    preview?.existing_account &&
                    me?.email === preview.email
                  ) && (
                    <PasswordField
                      label={
                        reset || (activate && !preview?.existing_account)
                          ? "Mật khẩu mới"
                          : "Mật khẩu"
                      }
                      fresh={reset || (activate && !preview?.existing_account)}
                    />
                  )}
                {(reset || (activate && !preview?.existing_account)) && (
                  <>
                    <p className="helper">
                      Từ 15 đến 128 ký tự. Có thể dán từ trình quản lý mật khẩu.
                    </p>
                    <PasswordField
                      name="confirm"
                      label="Nhập lại mật khẩu"
                      fresh
                    />
                  </>
                )}
                {activate && preview?.existing_account && (
                  <p className="helper">
                    {me?.email === preview.email
                      ? "Bạn đã đăng nhập đúng tài khoản nhận lời mời."
                      : "Dùng mật khẩu hiện tại của tài khoản nhận lời mời. Mật khẩu của bạn sẽ không thay đổi."}
                  </p>
                )}
                {!activate && !reset && !forgot && (
                  <button
                    type="button"
                    className="text-link forgot-link"
                    onClick={() => navigate("/forgot-password")}
                  >
                    Quên mật khẩu?
                  </button>
                )}
                <button className="full-width" disabled={busy}>
                  {busy
                    ? "Đang xử lý…"
                    : forgot
                      ? "Gửi hướng dẫn đặt lại"
                      : reset
                        ? "Lưu mật khẩu mới"
                        : activate
                          ? preview?.existing_account
                            ? "Chấp nhận lời mời"
                            : "Kích hoạt tài khoản"
                          : "Đăng nhập"}
                </button>
              </form>
            )}
            {activate && preview && (
              <button
                disabled={busy}
                className="text-link full-width"
                onClick={() =>
                  void perform(async () => {
                    await post("/api/invitations/report-incorrect", { token });
                    setDone(
                      "Đã báo thông tin không đúng. Quản trị viên sẽ kiểm tra và gửi lại lời mời.",
                    );
                  })
                }
              >
                Thông tin căn hộ không đúng?
              </button>
            )}
            {(forgot || reset) && (
              <button
                className="text-link full-width"
                onClick={() => navigate("/login")}
              >
                Quay lại đăng nhập
              </button>
            )}
            {!activate && !forgot && !reset && (
              <div className="onboarding-note">
                <strong>Chưa có tài khoản?</strong>
                <p>
                  Liên hệ quản trị viên qua kênh chung cư đã công bố để được xác
                  minh và nhận lời mời.
                </p>
              </div>
            )}
          </>
        )}
      </section>
    </main>
  );
}
