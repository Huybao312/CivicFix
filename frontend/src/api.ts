export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}
export async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers);
  if (options.method && options.method !== "GET") {
    const csrf = await request<{ token: string; headerName: string }>(
      "/api/auth/csrf",
    );
    headers.set(csrf.headerName, csrf.token);
  }
  const response = await fetch(path, {
    credentials: "same-origin",
    ...options,
    headers,
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok)
    throw new ApiError(
      response.status,
      data.message || "Không thể thực hiện yêu cầu. Vui lòng thử lại.",
    );
  return data as T;
}
export const post = <T = { ok: boolean }>(path: string, data: unknown = {}) =>
  request<T>(path, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(data),
  });
export async function login(email: string, password: string) {
  return request("/api/auth/login", {
    method: "POST",
    body: new URLSearchParams({ email, password }),
  });
}
export type Membership = {
  id: string;
  building_id: string;
  building_name: string;
  role: string;
};
export type Apartment = {
  link_id?: string;
  id: string;
  unit_number: string;
  floor?: string;
  building_id?: string;
  building_name?: string;
};
export type Me = {
  id: string;
  email: string;
  display_name: string;
  memberships: Membership[];
  apartments: Apartment[];
};
export const roleNames: Record<string, string> = {
  ADMIN: "Quản trị viên",
  RESIDENT: "Cư dân",
  MANAGER: "Quản lý phiếu",
  TECHNICIAN: "Kỹ thuật viên",
};
export const states: Record<string, string> = {
  INVITED: "Chưa kích hoạt",
  ACTIVE: "Hoạt động",
  DISABLED: "Vô hiệu hóa",
  PENDING: "Đang chờ",
  ACCEPTED: "Đã chấp nhận",
  EXPIRED: "Hết hạn",
  REVOKED: "Đã thu hồi",
  ENDED: "Đã kết thúc",
  VERIFIED: "Đã xác minh",
};
export function date(value?: string) {
  return value ? new Date(value).toLocaleString("vi-VN") : "—";
}
