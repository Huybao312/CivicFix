# CivicFix

CivicFix là dự án web responsive hỗ trợ cư dân báo sự cố kỹ thuật tại khu vực chung của chung cư, đồng thời giúp ban quản lý tiếp nhận, phân công và theo dõi quá trình xử lý.

**Trạng thái:** Đang thực hiện M2. Đã có bộ khung React/TypeScript, Spring Boot, PostgreSQL qua Docker Compose, migration Flyway tạo bảng `buildings` và CI kiểm tra Frontend/Backend. Frontend hiện hiển thị trang mẫu; các chức năng nghiệp vụ chưa được triển khai. Nhu cầu người dùng thực tế chưa được xác thực.

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

Profile `local` đọc `.env` dưới dạng Java properties, vì vậy nhập giá trị trực tiếp sau dấu `=`, không bọc trong dấu nháy. Thay mật khẩu trong `.env` sau khi database đã khởi tạo không tự đổi mật khẩu của PostgreSQL.

### 2. Chạy PostgreSQL

Tại gốc repo:

```bash
docker compose up -d db
docker compose ps
```

Chờ service `db` chuyển sang `healthy`. Database `civicfix` dùng user `civicfix` và được mở tại `127.0.0.1:5433`. Dữ liệu nằm trong named volume của Compose.

### 3. Chạy Backend

Mở Terminal riêng tại gốc repo:

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Chạy lệnh từ thư mục `backend` để profile `local` tìm thấy `../.env`. Flyway tự áp dụng migration khi ứng dụng khởi động. Backend mặc định chạy tại `http://localhost:8080`; hiện chưa có API nghiệp vụ, nên truy cập đường dẫn `/` có thể trả về HTTP 404.

### 4. Chạy Frontend

Mở Terminal khác tại gốc repo:

```bash
cd frontend
npm ci
npm run dev
```

Mở địa chỉ Vite in trong Terminal, mặc định là `http://localhost:5173`. Hiện trang mẫu Frontend chưa gọi API Backend.

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

Test Backend cần Docker đang chạy. Testcontainers tự tạo PostgreSQL riêng và Flyway chạy migration trên database thử nghiệm; không cần `.env` hoặc service `db` của Compose. Test hiện xác nhận bảng `buildings` được tạo, chưa bao phủ nghiệp vụ tài khoản hay phiếu sự cố.

Workflow [CI](.github/workflows/ci.yml) chạy Frontend lint/build và Backend verify khi push hoặc mở pull request. Có thể xem kết quả tại [GitHub Actions](https://github.com/Huybao312/CivicFix/actions).

## Bước tiếp theo

1. Tiếp tục [M2](docs/ROADMAP.md): tạo dữ liệu tài khoản, căn hộ, membership và liên kết cư dân–căn hộ; seed dữ liệu giả.
2. Làm luồng mời/kích hoạt, đăng nhập, liên kết cư dân–căn hộ và kiểm tra quyền ở backend theo [vòng đời tài khoản](docs/ACCOUNT_LIFECYCLE.md) và [ma trận quyền](docs/ACCESS_CONTROL.md).
3. Viết test cho một đường thành công và các ca vượt quyền trước khi đóng từng issue chức năng.

Đây là dự án cá nhân phục vụ học tập và portfolio. Những chính sách vận hành trong PRD chưa được một chung cư cụ thể xác nhận.

Các [milestone trên GitHub](https://github.com/Huybao312/CivicFix/milestones) được chia theo luồng chức năng. Mỗi issue chức năng chỉ hoàn thành khi giao diện, API, dữ liệu, phân quyền và kiểm thử liên quan cùng chạy được.
