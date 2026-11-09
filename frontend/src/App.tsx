import { useEffect, useState } from "react";
import type { Me } from "./api";
import { ApiError, post, request } from "./api";
import AuthPages from "./AuthPages";
import AdminPage from "./AdminPage";
import Workspace from "./Workspace";
import { Notice } from "./ui";
import "./App.css";

function App() {
  const [path, setPath] = useState(window.location.pathname);
  const [token] = useState(
    () => new URLSearchParams(window.location.hash.slice(1)).get("token") || "",
  );
  const [me, setMe] = useState<Me | null>(null),
    [loading, setLoading] = useState(true),
    [error, setError] = useState("");
  function navigate(next: string) {
    window.history.pushState({}, "", next);
    setPath(next);
    window.scrollTo(0, 0);
  }
  async function refresh() {
    try {
      setMe(await request<Me>("/api/auth/me"));
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) setMe(null);
      else throw e;
    }
  }
  useEffect(() => {
    if (window.location.hash)
      window.history.replaceState({}, "", window.location.pathname);
    void request<Me>("/api/auth/me")
      .then(setMe)
      .catch((e) => {
        if (!(e instanceof ApiError && e.status === 401))
          setError(
            "Không kết nối được với CivicFix. Kiểm tra Backend rồi tải lại trang.",
          );
      })
      .finally(() => setLoading(false));
    const change = () => setPath(window.location.pathname);
    window.addEventListener("popstate", change);
    return () => window.removeEventListener("popstate", change);
  }, []);
  return (
    <>
      <a className="skip-link" href="#content">
        Đi tới nội dung
      </a>
      <header className="site-header">
        <a
          className="brand"
          href="/"
          onClick={(e) => {
            e.preventDefault();
            navigate("/");
          }}
        >
          <svg width="28" height="28" viewBox="0 0 28 28" aria-hidden="true">
            <path
              d="M5 23V8l9-4 9 4v15M10 23v-6h8v6M10 10h2m4 0h2M10 13h2m4 0h2"
              fill="none"
              stroke="currentColor"
              strokeWidth="1.7"
            />
          </svg>
          CivicFix
        </a>
        <span className="demo-label">Bản demo · dữ liệu giả</span>
        {me && (
          <div className="header-account">
            <span>{me.display_name}</span>
            <button
              className="secondary"
              onClick={() => {
                void post("/api/auth/logout")
                  .then(() => {
                    setMe(null);
                    navigate("/login");
                  })
                  .catch((e) => setError(e.message));
              }}
            >
              Đăng xuất
            </button>
          </div>
        )}
      </header>
      <div id="content">
        {loading ? (
          <main className="loading">
            <Notice>Đang mở CivicFix…</Notice>
          </main>
        ) : error ? (
          <main className="loading">
            <Notice error>{error}</Notice>
            <button onClick={() => window.location.reload()}>
              Tải lại trang
            </button>
          </main>
        ) : [
            "/activate",
            "/forgot-password",
            "/reset-password",
            "/login",
          ].includes(path) || !me ? (
          <AuthPages
            key={path}
            path={path}
            token={token}
            me={me}
            navigate={navigate}
            refresh={refresh}
          />
        ) : path !== "/workspace" &&
          !path.startsWith("/issues") &&
          me.memberships.some((m) => m.role === "ADMIN") ? (
          <AdminPage me={me} refresh={refresh} />
        ) : (
          <Workspace me={me} />
        )}
      </div>
      <footer className="site-footer">
        CivicFix · Kết nối cư dân và người phụ trách
      </footer>
    </>
  );
}
export default App;
