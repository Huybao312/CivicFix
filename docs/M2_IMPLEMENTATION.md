# M2 — Tài khoản, căn hộ xác minh và quyền truy cập

Phạm vi: triển khai local cho milestone M2. Chức năng tạo/xử lý phiếu, ảnh và thông báo nghiệp vụ chưa thuộc bản này. Hướng dẫn khởi động và tài khoản demo nằm trong [README](../README.md).

## 1. Những gì chạy được

| Task M2 | Kết quả và nơi triển khai | Kiểm chứng |
|---|---|---|
| Nền tảng | React/TypeScript, Spring Boot, PostgreSQL, Compose, Flyway; ba job CI | Frontend lint/build, Maven verify, Playwright |
| Danh tính và căn hộ | V1–V5; V6 bổ sung invitation, reset token, phiên bản xác thực và audit | Schema tests trên PostgreSQL Testcontainers |
| Seed | Profile `local`: chung cư An Bình, căn 03–08, bốn tài khoản/vai trò giả; cư dân thuộc căn 03 | E2E khởi động từ volume riêng mới |
| Mời cư dân | Admin chọn căn hộ và căn cứ xác minh; xem lại trước khi gửi; chống bấm/gửi đồng thời; gửi lại, thu hồi, báo sai căn hộ | API integration và E2E với Mailpit |
| Kích hoạt | Tự đặt mật khẩu; token có hạn, dùng một lần; tài khoản có sẵn phải đăng nhập đúng danh tính, giữ mật khẩu cũ | Test token hết hạn/thu hồi/phát lại/cạnh tranh và sai danh tính |
| Khôi phục | Thông báo quên mật khẩu chung; token reset riêng; đổi mật khẩu thu hồi phiên, gửi email xác nhận | API và E2E |
| Quyền và căn hộ | Role + building kiểm tra ở backend; xác minh/kết thúc liên kết; kết thúc membership; vô hiệu hóa; cho phép kích hoạt lại có lý do | Test vượt quyền, khác chung cư, giả actor, phiên cũ và lịch sử |
| Giao diện | Login, activation, forgot/reset, account administration, invitations, audit, workspace ba vai trò | Trình duyệt desktop, tablet và mobile |
| Tài liệu | Chạy từ clone mới với `.env.example`, API và quyết định triển khai | README, tài liệu này, workflow CI |

Các màn hình nghiệp vụ cư dân/quản lý/kỹ thuật viên hiện giải thích rõ chức năng phiếu sẽ có từ M3. Không dùng số liệu phiếu giả để tuyên bố dashboard nghiệp vụ đã hoàn tất.

## 2. Quyết định xác thực và bảo vệ dữ liệu

- Dùng Spring Security session phía server, cookie `JSESSIONID` HttpOnly, SameSite=Lax, hết hạn sau 30 phút không hoạt động. Đổi session ID khi đăng nhập; logout dùng POST có CSRF. Không lưu token đăng nhập trong localStorage.
- Mọi POST dùng CSRF token lấy từ `/api/auth/csrf`. Frontend dùng Vite proxy `/api`, cùng origin với trình duyệt. Production HTTPS, Secure cookie và phục vụ SPA sẽ cần cấu hình ở giai đoạn deploy.
- Password dùng PBKDF2 mặc định Spring Security v5.8: salt ngẫu nhiên, 310.000 vòng, SHA-256. Không ghi mật khẩu/token vào log hoặc response quản trị.
- Mật khẩu 15–128 ký tự Unicode, hỗ trợ passphrase, dán và password manager; chặn một số mật khẩu phổ biến và chuỗi lặp quá đơn giản. Danh sách offline không thay thế dịch vụ kiểm tra toàn bộ mật khẩu bị lộ.
- Token mời/reset được sinh từ 32 byte ngẫu nhiên; database chỉ lưu SHA-256. Link dùng fragment, frontend xóa fragment sau khi đọc; API nhận token trong body, không trong query string.
- Lời mời có hạn 24 giờ; gửi lại cách nhau ít nhất 60 giây và đổi token. Reset có hạn 30 phút; yêu cầu mới làm token reset trước mất hiệu lực. Tiêu thụ token trong transaction với khóa user/token để chặn phát lại và cạnh tranh.
- `users.auth_version` được kiểm tra lại mỗi request. Đổi mật khẩu, kết thúc membership/liên kết hoặc khóa tài khoản làm phiên cũ bị từ chối ở request kế tiếp. Quyền role/building và khoảng thời gian hiệu lực cũng được đọc từ database mỗi lần thao tác.
- Danh tính ACTIVE có thể đăng nhập để nhận lời mời mới khi chưa có membership hiệu lực; không nhận quyền nghiệp vụ chỉ nhờ đăng nhập. ADMIN không mặc nhiên có quyền MANAGER/TECHNICIAN/RESIDENT.
- Người xác minh là admin đã xác thực phía server. Client không tự cấp vai trò hoặc sửa actor. Kết thúc liên kết giữ bản ghi lịch sử, không xóa.
- Audit có actor, đối tượng, building, lý do và thời điểm; trigger chặn UPDATE/DELETE qua các câu lệnh thông thường. Đây không phải kho audit độc lập chống quản trị viên database can thiệp.
- Vô hiệu hóa toàn cục bị chặn nếu user còn membership chưa kết thúc ở chung cư khác. Cho phép kích hoạt lại là thao tác có audit riêng; không tự khôi phục membership hoặc căn hộ cũ.

## 3. API

Tất cả đường dẫn có tiền tố `/api`. JSON là định dạng mặc định, ngoại trừ login là `application/x-www-form-urlencoded` với `email` và `password`. API trả `401` khi chưa đăng nhập/phiên bị thu hồi, `403` khi thiếu quyền hoặc CSRF, `404` cho đối tượng ngoài phạm vi, `409` cho xung đột nghiệp vụ, `410` cho link không còn dùng được, `422` cho dữ liệu sai và `429` khi vượt giới hạn.

### Tài khoản và token

| Method | Đường dẫn | Mục đích |
|---|---|---|
| GET | `/auth/csrf` | Lấy tên header và CSRF token; tạo phiên ẩn danh nếu cần |
| POST | `/auth/login` | Đăng nhập, thông báo lỗi chung |
| POST | `/auth/logout` | Hủy phiên |
| GET | `/auth/me` | Danh tính, membership hiệu lực và căn hộ xác minh được phép dùng |
| POST | `/auth/forgot-password` | `{email}`; luôn phản hồi chung với email hợp lệ về cú pháp |
| POST | `/auth/reset-password` | `{token,password}` |
| POST | `/invitations/preview` | `{token}`; thông tin tối thiểu trong lời mời còn hạn |
| POST | `/invitations/accept` | `{token,password?}`; password chỉ cho danh tính chưa kích hoạt |
| POST | `/invitations/report-incorrect` | `{token}`; ngừng kích hoạt và tạo dấu vết để admin kiểm tra |

### Quản trị theo chung cư

Các đường dẫn dưới đây nằm trong `/buildings/{buildingId}` và yêu cầu ADMIN tại chính chung cư đó, ngoại trừ workspace kiểm tra vai trò tương ứng.

| Method | Đường dẫn | Dữ liệu/chức năng |
|---|---|---|
| GET | `/accounts`, `/apartments`, `/invitations`, `/audit` | Danh sách có phạm vi, không có hash mật khẩu/token |
| POST | `/invitations` | `{email,displayName,apartmentId,verificationSource,validFrom?}` |
| POST | `/invitations/{id}/resend` | Token mới cho lời mời đang chờ |
| POST | `/invitations/{id}/revoke` | `{reason}` |
| POST | `/memberships/{id}/end` | `{reason}`; kết thúc quyền, liên kết cư dân và lời mời liên quan |
| POST | `/accounts/{id}/disable` | `{reason}`; khóa danh tính, token reset và quyền liên quan |
| POST | `/accounts/{id}/enable` | `{reason}`; cho phép dùng danh tính lại, không khôi phục quyền cũ |
| GET | `/accounts/{id}/links` | Liên kết căn hộ hiện tại và lịch sử trong chung cư |
| POST | `/links` | `{userId,apartmentId,verificationSource}`; cần resident membership hiệu lực |
| POST | `/links/{id}/end` | `{reason}` |
| GET | `/workspace/{role}` | Chỉ RESIDENT/MANAGER/TECHNICIAN đúng role và building |

`verificationSource`: `RESIDENT_LIST` hoặc `OPERATOR_CONFIRMATION`. Giao diện dùng nhãn tiếng Việt; backend kiểm tra enum. Token có chủ đích cho phép người nhận xem căn hộ trong lời mời, nên không chia sẻ link hoặc chụp toàn bộ email có token lên nơi công khai.

## 4. Email và giới hạn môi trường demo

SMTP sender cố định tới loopback `127.0.0.1`, cổng `CIVICFIX_SMTP_PORT` (mặc định 1025). Chỉ nhận địa chỉ `@demo.civicfix.local`; Mailpit không được cấu hình relay. Link chỉ dùng origin localhost/127.0.0.1 (`CIVICFIX_PUBLIC_ORIGIN`, mặc định `http://localhost:5173`).

Mời/gửi lại và xác nhận đổi mật khẩu báo lỗi nếu SMTP thất bại, transaction nghiệp vụ rollback. SMTP không thể tham gia transaction PostgreSQL: nếu mail đã nhận nhưng DB commit thất bại thì link không dùng được; admin gửi lại sau khi kiểm tra. Demo chưa có transactional outbox.

Quên mật khẩu gửi email sau khi commit qua hàng đợi trong bộ nhớ (một worker, tối đa 100 việc), tránh phản hồi công khai phụ thuộc thành bại SMTP. Restart hoặc SMTP lỗi có thể mất email đang chờ; người dùng cần yêu cầu lại. Chưa có hàng đợi bền vững/retry cho môi trường thật.

Giới hạn trong một JVM: login 15 lần/email+IP/15 phút và 100 lần/IP/15 phút; quên mật khẩu 3 lần/email/giờ và 30 lần/IP/giờ; các thao tác public invitation 90 lần/IP/phút. Restart xóa bộ đếm. Đây chưa phải giải pháp chống lạm dụng cho nhiều instance hoặc Internet công khai.

## 5. Đối chiếu tiêu chí chấp nhận

- AC-27–35, AC-37, AC-39–40: có triển khai API/giao diện và test các nhánh chính cùng ca từ chối quan trọng.
- AC-36: M2 kiểm tra thu hồi phiên, membership và căn hộ, giữ lịch sử; quyền API ảnh/thông báo và snapshot phiếu sẽ được kiểm chứng khi những tài nguyên đó được triển khai.
- AC-38: M2 kiểm tra kết thúc liên kết cũ, xác minh liên kết mới và phiên cũ mất hiệu lực. Snapshot căn hộ trên phiếu trước/sau chuyển căn thuộc M3. Không đánh dấu toàn bộ AC-38 hoàn tất ở M2.

## 6. Kiểm chứng và cách chạy lại

Tại gốc repo:

```bash
cd backend
./mvnw --batch-mode verify
```

Backend dùng PostgreSQL Testcontainers độc lập, gồm 20 schema tests và 43 account integration tests. Test kiểm tra response, trạng thái database, email mock và quyền; có tình huống hai request tạo/tiêu thụ lời mời đồng thời.

Tại thư mục frontend:

```bash
npm ci
npm run lint
npm run build
npx playwright install chromium
npm run test:e2e
```

Bốn kịch bản trình duyệt dùng API thật, PostgreSQL mới và Mailpit: mời → kích hoạt → đăng nhập → reset → kết thúc quyền; link sai/lỗi login trên điện thoại; báo sai căn hộ và admin kiểm tra; workspace quản lý/kỹ thuật viên. Kiểm tra cookie phiên, đổi session ID và tràn ngang ở desktop/tablet/mobile.

E2E chỉ xóa tài nguyên thuộc project `civicfix-e2e`, không xóa volume local `civicfix`. Không chạy hai lượt E2E đồng thời vì dùng cùng project/cổng thử nghiệm.

Kết quả kiểm chứng local ngày 08/11/2026:

| Kiểm tra | Kết quả |
|---|---|
| Maven `verify` | 63 tests, 0 failures, 0 errors, 0 skipped; BUILD SUCCESS |
| Frontend lint | Không có lỗi |
| Frontend production build | Thành công |
| Playwright + PostgreSQL + Mailpit | 4/4 kịch bản thành công |

M2 đã merge qua [PR #12](https://github.com/Huybao312/CivicFix/pull/12), commit `d86b723`; [CI trên main](https://github.com/Huybao312/CivicFix/actions/runs/42492225106) có cả ba job thành công. Issue #3 và milestone M2 đã đóng.

Ảnh từ ứng dụng thực tế: [desktop, mobile và kích hoạt](../design/m2-implemented/README.md).

## 7. Nguồn tài nguyên

`backend/src/main/resources/common-passwords.txt` lấy từ [SecLists — 10k-most-common.txt](https://github.com/danielmiessler/SecLists/blob/master/Passwords/Common-Credentials/10k-most-common.txt). Giữ bản license gốc trong `common-passwords-LICENSE.txt`. Không dùng tài nguyên này để suy luận rằng người dùng thực có mật khẩu an toàn tuyệt đối.
