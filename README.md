# CivicFix

CivicFix là dự án web responsive hỗ trợ cư dân báo sự cố kỹ thuật tại khu vực chung của chung cư, đồng thời giúp ban quản lý tiếp nhận, phân công và theo dõi quá trình xử lý.

**Trạng thái** — M2 và M3 đã merge. Nhánh M4 bổ sung tiếp nhận, hỏi–đáp, từ chối/khôi phục, ưu tiên, phân công và thông báo trong ứng dụng. Kỹ thuật viên có trang Việc của tôi. Chi tiết triển khai và cách thử nằm trong [tài liệu M4](docs/M4_IMPLEMENTATION.md).

## Bài toán

Báo cáo sự cố thường đến qua nhiều kênh như điện thoại, nhóm chat và trao đổi trực tiếp. Thông tin có thể bị phân tán, báo trùng hoặc thiếu người chịu trách nhiệm. CivicFix hướng đến một hồ sơ xử lý thống nhất, có lịch sử và tiến độ rõ ràng cho các bên liên quan.

## Các chức năng dự kiến

- Một chung cư, sự cố kỹ thuật ở khu vực chung.
- Cư dân báo cáo và theo dõi qua web trên điện thoại hoặc máy tính.
- Phiếu lưu người báo và căn hộ đã xác minh để ban quản lý đối chiếu; vị trí thiết bị hỏng được ghi riêng.
- Ban quản lý nhập báo cáo nhận từ điện thoại, chat hoặc trực tiếp; yêu cầu bổ sung thông tin, xử lý báo trùng và phân công.
- Kỹ thuật viên xem việc được giao, cập nhật công việc và đề xuất hoàn thành.
- Ban quản lý xác nhận kết quả; cư dân phản hồi nếu sự cố vẫn chưa được khắc phục.
- Người có tài khoản nhận thông báo kết quả trong ứng dụng; với báo cáo nhập hộ, ban quản lý có việc liên hệ và ghi nhận đã báo lại qua kênh phù hợp.
- Lịch sử xử lý và quyền truy cập được kiểm soát theo vai trò trong chung cư.

CivicFix không thay thế kênh liên hệ khẩn cấp của chung cư.

## Tài liệu

- [Product Requirements Document](docs/PRD.md): mục tiêu, người dùng, luồng nghiệp vụ, quyền truy cập, tiêu chí chấp nhận và các giả định cần xác minh.
- [Vòng đời tài khoản cư dân](docs/ACCOUNT_LIFECYCLE.md): quản trị viên mời, cư dân kích hoạt, khôi phục mật khẩu và kết thúc quyền căn hộ.
- [Roadmap và milestone](docs/ROADMAP.md): thứ tự thực hiện, đầu ra của từng mốc, điều kiện hoàn thành và việc nên làm ngay.
- [Sơ đồ luồng nghiệp vụ](docs/FLOWS.md): đường đi của báo cáo qua web và nhập hộ, phân loại, xử lý, báo kết quả, đóng và mở lại phiếu.
- [Ma trận quyền](docs/ACCESS_CONTROL.md): ai được thao tác và đọc từng loại dữ liệu, ảnh, lịch sử và thông báo.
- [State machine và tiêu chí chấp nhận](docs/ACCEPTANCE_CRITERIA.md): điều kiện chuyển trạng thái và các ca để viết test.
- [Sổ giả định và kịch bản mô phỏng](docs/ASSUMPTIONS.md): phân biệt căn cứ công khai, suy luận và các điều chưa thể xác minh.
- [Bản xem trước màn hình cư dân](design/resident-preview/README.md) và [vòng đời tài khoản](design/account-preview/README.md): ảnh thiết kế để duyệt trước khi code.

## Chạy trên máy cá nhân

### Công cụ cần có

- Node.js 24 và npm.
- JDK 21.
- Docker đang chạy, kèm lệnh `docker compose`.

Backend có Maven Wrapper (`mvnw`), không cần cài Maven riêng. CI dùng Node.js 24 và JDK 21.

Các lệnh dưới đây bắt đầu tại gốc repo: thư mục chứa `README.md`, `compose.yaml`, `frontend/` và `backend/`.

### 1. Chuẩn bị cấu hình local

Với bản clone mới, tạo `.env` từ file mẫu:

```bash
cp .env.example .env
```

Mở `.env`, thay giá trị mẫu của `POSTGRES_PASSWORD` bằng mật khẩu dùng riêng cho database local. Nếu đã có `.env`, giữ file hiện tại. `.env` được Git bỏ qua; `.env.example` chỉ chứa giá trị mẫu.

Docker Compose đọc `.env`; profile `local` của Spring Boot lấy thông tin kết nối từ container đang chạy. Backend không đọc lại `.env` dưới dạng Java properties. Có thể dùng dấu nháy đơn để giữ nguyên ký tự đặc biệt, ví dụ `POSTGRES_PASSWORD='local$example'` (chỉ là ví dụ, hãy chọn mật khẩu riêng). Thay mật khẩu trong `.env` sau khi database đã khởi tạo không tự đổi mật khẩu của PostgreSQL.

### 2. Chạy PostgreSQL

Tại gốc repo:

```bash
docker compose up -d db mailpit
docker compose ps
```

Chờ service `db` chuyển sang `healthy`. Database `civicfix` dùng user `civicfix` và được mở tại `127.0.0.1:5433`. Dữ liệu nằm trong named volume của Compose. Mailpit nhận email thử nghiệm tại SMTP `127.0.0.1:1025`; mở hộp thư tại `http://localhost:8025`. Không cấu hình gửi email ra Internet.

### 3. Chạy Backend

Mở Terminal riêng tại gốc repo:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Chạy lệnh từ thư mục `backend` để profile `local` tìm thấy `../compose.yaml`. PostgreSQL phải được khởi động ở bước 2; Backend chỉ lấy thông tin kết nối từ container, không tự khởi động hay dừng Compose. Cổng, user và mật khẩu được lấy từ cấu hình container thực tế. Flyway tự áp dụng migration khi ứng dụng khởi động. Backend mặc định chạy tại `http://localhost:8080`; API tài khoản nằm dưới `/api`; giao diện được mở từ Vite, không từ cổng Backend.

### 4. Chạy Frontend

Mở Terminal khác tại gốc repo:

```bash
cd frontend
npm ci
npm run dev
```

Mở địa chỉ Vite in trong Terminal, mặc định là `http://localhost:5173`. Vite chuyển tiếp `/api` đến Backend qua proxy cùng origin, để cookie phiên và CSRF hoạt động. Nếu đổi cổng Backend, đặt biến môi trường `CIVICFIX_API_TARGET` cho Vite.

### Tài khoản demo và luồng thử

Chỉ profile `local` tự seed chung cư An Bình, các căn 03–08 và bốn tài khoản giả:

| Vai trò | Email |
|---|---|
| Quản trị viên | `admin@demo.civicfix.local` |
| Quản lý phiếu | `manager@demo.civicfix.local` |
| Kỹ thuật viên | `technician@demo.civicfix.local` |
| Cư dân, căn 03 | `resident@demo.civicfix.local` |

Mật khẩu khởi tạo chung: `CivicFix demo passphrase 2026!`. Đây là dữ liệu demo công khai, chỉ dùng trên máy cá nhân. Seed không đặt lại mật khẩu, bật lại tài khoản bị khóa hoặc khôi phục quyền đã kết thúc khi khởi động lại.

1. Đăng nhập admin ở `http://localhost:5173`; chọn **Mời cư dân**.
2. Nhập tên giả, email mới có đuôi `@demo.civicfix.local`, căn hộ và căn cứ xác minh; kiểm tra rồi gửi.
3. Mở Mailpit, tìm email đó; mở link trong cửa sổ riêng tư để thử như cư dân mới.
4. Cư dân kiểm tra căn hộ, tự đặt mật khẩu và đăng nhập. Tài khoản có sẵn phải đăng nhập đúng người nhận trước khi chấp nhận; mật khẩu hiện tại giữ nguyên.
5. Thử **Quên mật khẩu**; dùng link mới trong Mailpit. Link dùng một lần, có hạn 30 phút; đổi mật khẩu làm phiên cũ mất hiệu lực.
6. Tại admin, mở chi tiết tài khoản để kết thúc membership, xác minh/kết thúc liên kết căn hộ hoặc vô hiệu hóa tài khoản. Nhật ký thay đổi giữ actor, đối tượng và lý do.

Lời mời có hạn 24 giờ. Gửi lại sau ít nhất 60 giây tạo link mới và vô hiệu hóa link cũ. Báo sai căn hộ dừng kích hoạt và xuất hiện trong danh sách lời mời để admin kiểm tra. Người nhận không tự thay căn hộ.

Cư dân mở **Phiếu của tôi**, chọn **Báo sự cố khu vực chung**, nhập vị trí và mô tả rồi gửi. Phiếu mới có trạng thái “Đã ghi nhận”, căn hộ khi báo và lịch sử đầu tiên. Quản lý thấy phiếu cùng chung cư; cư dân khác không đọc được. Ảnh được hoãn đến M5. Xem [chi tiết M3](docs/M3_IMPLEMENTATION.md).

Để thử M4, đăng nhập quản lý, mở phiếu rồi chọn **Yêu cầu bổ sung**. Cư dân mở **Thông báo**, vào phiếu và trả lời câu hỏi. Quản lý có thể tiếp nhận, từ chối/khôi phục, đổi ưu tiên và phân công. Sau phân công, đăng nhập kỹ thuật viên để mở **Việc của tôi**. Các lý do công khai hiển thị trong lịch sử người báo, lý do đổi ưu tiên được quản lý đọc trong lịch sử nội bộ.

### 5. Dừng ứng dụng

Nhấn `Ctrl+C` trong từng Terminal chạy Frontend và Backend. Tại gốc repo, dừng database bằng:

```bash
docker compose down
```

Lệnh này giữ lại dữ liệu trong volume cho lần chạy tiếp theo.

## Kiểm tra trước khi commit

Frontend, chạy từ gốc repo:

```bash
cd frontend
npm ci
npm run lint
npm run build
```

Backend, chạy trong một Terminal tại gốc repo:

```bash
cd backend
./mvnw --batch-mode verify
```

Test Backend cần Docker đang chạy. Testcontainers tự tạo PostgreSQL riêng và Flyway chạy migration trên database thử nghiệm; không cần `.env` hoặc service `db` của Compose. Bộ test gồm ràng buộc schema và API vòng đời tài khoản: CSRF, session, phạm vi chung cư, invitation/reset token, gửi trùng đồng thời, thu hồi quyền và các ca từ chối. Nhóm M3 kiểm tra tạo/xem phiếu, snapshot, IDOR, replay và rollback; M4 kiểm tra quyết định điều phối, phân công đồng thời, thông báo và quyền kỹ thuật viên.

Kiểm thử trình duyệt với PostgreSQL và Mailpit riêng, chạy từ thư mục `frontend`:

```bash
npx playwright install chromium
npm run test:e2e
```

Script tự khởi động môi trường `civicfix-e2e`, Backend và Vite ở các cổng riêng, chạy Playwright rồi xóa riêng container/volume E2E. Cần các cổng `15433`, `11025`, `18025`, `18080`, `4173` còn trống. Không dùng dữ liệu hoặc mật khẩu `.env` local. Nếu đã có Google Chrome, có thể dùng `CIVICFIX_BROWSER_CHANNEL=chrome npm run test:e2e`.

Workflow [CI](.github/workflows/ci.yml) chạy ba job: Frontend lint/build, Backend verify và Account and issue flows in browser khi push hoặc mở pull request. Có thể xem kết quả tại [GitHub Actions](https://github.com/Huybao312/CivicFix/actions).

## Bước tiếp theo

1. Rà soát nhánh M4 và kết quả CI trên commit mới trước khi merge.
2. Tiếp theo triển khai M5 với cập nhật công việc kỹ thuật, xác nhận kết quả, ảnh riêng tư và phản hồi của cư dân.
3. Giữ nguyên yêu cầu kiểm thử, phân quyền và audit trong từng chức năng mới.

Chi tiết API, các quyết định triển khai và bằng chứng M2 nằm trong [M2 implementation](docs/M2_IMPLEMENTATION.md).

Đây là dự án cá nhân phục vụ học tập và portfolio. Những chính sách vận hành trong PRD chưa được một chung cư cụ thể xác nhận.

Các [milestone trên GitHub](https://github.com/Huybao312/CivicFix/milestones) được chia theo luồng chức năng. Mỗi issue chức năng chỉ hoàn thành khi giao diện, API, dữ liệu, phân quyền và kiểm thử liên quan cùng chạy được.
