import { useRemote } from "./hooks";
import { date } from "./api";
import { Notice } from "./ui";

type Notification = {
  id: string;
  issue_id: string;
  message: string;
  read_at: string | null;
  created_at: string;
};
export default function Notifications({
  building,
  href,
}: {
  building: string;
  href: (id?: string) => string;
}) {
  const { data, error, busy, reload } = useRemote<Notification[]>(
    `/api/buildings/${building}/notifications`,
  );
  const unread = data?.filter((item) => !item.read_at).length || 0;
  return (
    <details className="notification-tray">
      <summary>Thông báo{unread > 0 && ` · ${unread} chưa đọc`}</summary>
      {busy ? (
        <Notice>Đang tải thông báo…</Notice>
      ) : error ? (
        <>
          <Notice error>{error}</Notice>
          <button type="button" onClick={reload}>
            Tải lại thông báo
          </button>
        </>
      ) : data?.length ? (
        <ul>
          {data.map((item) => (
            <li key={item.id}>
              <a
                href={`${href(item.issue_id)}&notification=${encodeURIComponent(item.id)}`}
              >
                <span>
                  {!item.read_at && (
                    <strong className="unread-label">Chưa đọc · </strong>
                  )}
                  {item.message}
                </span>
                <time>{date(item.created_at)}</time>
              </a>
            </li>
          ))}
        </ul>
      ) : (
        <p>Chưa có thông báo.</p>
      )}
    </details>
  );
}
