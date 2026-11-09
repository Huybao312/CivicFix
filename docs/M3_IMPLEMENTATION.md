# M3 — Cư dân tạo, xem phiếu và lưu lịch sử ban đầu

## Luồng đã triển khai

Cư dân đăng nhập → Phiếu của tôi → Báo sự cố khu vực chung → kiểm tra phạm vi và hướng dẫn nguy hiểm → nhập vị trí/mô tả → gửi → xem mã phiếu, căn hộ khi báo và lịch sử đầu tiên. Quản lý đọc danh sách/chi tiết cùng chung cư; cư dân khác và admin chỉ có quyền quản trị không đọc được phiếu.

Dựa trên prototype đã duyệt trong `design/resident-preview`. Giữ danh sách gọn, không có bốn thẻ thống kê hoặc thanh trạng thái lặp lại. Trang danh sách trả 20 phiếu/lần, có tìm kiếm và phân trang; form và chi tiết dùng được trên điện thoại.

## Phạm vi và quyết định

- Chỉ tiếp nhận `COMMON_AREA`. Form giải thích thiết bị riêng trong căn hộ ngoài phạm vi; API từ chối `PRIVATE_APARTMENT`. Hệ thống không tự suy luận đúng/sai phạm vi từ văn bản tự do.
- Hiển thị hướng dẫn nguy hiểm trước form. Bản demo chưa có số khẩn cấp được xác nhận; không bịa số điện thoại hay trình bày web như kênh ứng cứu.
- M3 nhận mô tả chữ. Upload ảnh được hoãn đến M5 để đi cùng storage riêng tư, giới hạn tệp và kiểm tra quyền. Không có nút upload hoặc URL ảnh giả.
- Tiếp nhận, yêu cầu bổ sung và phân công ở M4; xử lý, kết quả và thông báo ở M5. M3 chỉ tạo `SUBMITTED` và lịch sử ban đầu; “đã ghi nhận” chưa đồng nghĩa được tiếp nhận hoặc đang sửa.
- Quản lý cũng có thể tự báo dưới danh tính mình theo ma trận quyền; nhập hộ người khác chưa có, thuộc M6.

## API

Yêu cầu đăng nhập bằng session, mọi POST cần CSRF như M2.

| Method | Endpoint | Hành vi |
|---|---|---|
| POST | `/api/buildings/{building}/issues` | Tạo phiếu; bắt buộc header `Idempotency-Key` là UUID |
| GET | `/api/buildings/{building}/issues?page=0&size=20&q=` | Danh sách theo quyền, `size` 1–50, `page` 0–50000, `q` tối đa 120 ký tự |
| GET | `/api/buildings/{building}/issues/{id}` | Chi tiết đúng phạm vi; ngoài phạm vi trả 404 |
| GET | `/api/buildings/{building}/issues/{id}/events` | Lịch sử công khai; kiểm tra cùng quyền đối tượng |

Body tạo phiếu:

```json
{
  "scope": "COMMON_AREA",
  "locationDescription": "Hành lang tầng 3, gần thang máy B",
  "description": "Đèn chớp liên tục vào buổi tối, hành lang khá tối.",
  "category": "ELECTRICAL",
  "apartmentLinkId": null
}
```

Vị trí 3–300 ký tự, mô tả 10–3000, kiểm tra sau khi trim. `category` tùy chọn, mặc định `OTHER`; chấp nhận `OTHER`, `ELECTRICAL`, `WATER`, `ELEVATOR`. Trường không được định nghĩa bị từ chối: không nhận `reported_by`, snapshot căn hộ, `status`, `assigned_to` hoặc tệp từ client.

POST thành công/replay đều trả 200 với cùng biểu diễn chi tiết. Danh sách trả `{items,page,size,hasMore}`; không trả hash hoặc idempotency key. 401 cho phiên không hợp lệ, 403 cho role thiếu quyền thao tác/danh sách, 404 cho phiếu ngoài phạm vi, 409 cho cùng key nhưng khác nội dung, 422 cho dữ liệu/header/tham số sai.

## Danh tính, căn hộ và lịch sử

Backend lấy `reported_by` từ session. Snapshot giữ `reporter_name_at_report`, `apartment_id_at_report` và `apartment_label_at_report` trong bản ghi phiếu, độc lập với `location_description`.

- Có đúng một liên kết căn hộ hiệu lực: backend tự chọn nếu client không chọn.
- Có nhiều liên kết: chọn `apartmentLinkId` thuộc danh sách hiệu lực của chính người báo tại chung cư đó; không nhận số căn hộ tự khai.
- Chưa có liên kết: vẫn nhận báo cáo, snapshot căn hộ null, giao diện quản lý ghi “Căn hộ chưa xác minh” (AC-02).
- Chuyển căn hoặc đổi nhãn căn hộ không đổi snapshot phiếu cũ. Kết thúc membership chặn đọc phiếu cũ của người báo; quản lý vẫn thấy lịch sử.

Tạo phiếu khóa user theo cùng thứ tự với thao tác quản trị M2, kiểm tra lại membership và căn hộ trong transaction. PostgreSQL ghi phiếu + event `ISSUE_SUBMITTED` cùng giao dịch. Trigger chặn UPDATE/DELETE event thông thường; không tuyên bố chống quản trị viên database can thiệp.

Mã `CF-...` là định danh hiển thị dùng sequence, không phải số lượng phiếu. Rollback có thể làm số bị khuyết. Quyền đọc dựa trên user/role/building/đối tượng, không dựa vào việc mã hoặc UUID có khó đoán hay không.

## Chống gửi lặp

Key được ràng buộc theo `(building_id,reported_by,idempotency_key)`, kết hợp fingerprint nội dung đã chuẩn hóa. Hai request đồng thời được tuần tự hóa qua khóa user: cùng key/nội dung trả cùng phiếu và một event; khác nội dung trả 409. Replay vẫn kiểm tra quyền hiện tại, không dùng key để lấy lại dữ liệu sau khi hết quyền.

Frontend giữ nguyên key và payload nếu mất kết nối/lỗi server chưa rõ đã commit hay chưa; khóa chỉnh sửa và cho gửi lại cùng nội dung. Không ghi mô tả hoặc thông tin cư dân vào localStorage. Bản M3 chưa lưu draft qua reload/đóng tab: khi chưa rõ kết quả, kiểm tra danh sách trước khi mở báo cáo mới. Hai key khác nhau là hai command khác nhau; phát hiện/gộp cùng sự cố vật lý thuộc M7.

## Quyền đọc

- RESIDENT hiệu lực: chỉ phiếu mình báo, ở đúng chung cư.
- MANAGER hiệu lực: mọi phiếu cùng chung cư, có tên người báo và căn hộ tại thời điểm báo.
- ADMIN đơn thuần: không được đọc/tạo phiếu; cần thêm role nghiệp vụ phù hợp.
- TECHNICIAN đơn thuần: chưa được đọc phiếu chưa phân công. Luồng phân công sẽ có ở M4.
- Một tài khoản có nhiều role: hợp quyền đang hiệu lực ở cùng chung cư theo ma trận quyền.

M3 chỉ có event công khai. Khi M4 thêm lịch sử nội bộ, phải phân loại và lọc ở backend trước khi trả cho cư dân; không gửi hết rồi ẩn bằng CSS.

## Kiểm chứng

Chạy từ `backend`: `./mvnw --batch-mode verify`.

Chạy từ `frontend`: `npm run lint`, `npm run build`, `npm run test:e2e` (hoặc `CIVICFIX_BROWSER_CHANNEL=chrome npm run test:e2e` nếu dùng Chrome đã cài). E2E vẫn dùng project/volume riêng như M2; không xóa dữ liệu local.

Nhóm test M3 bao phủ: căn hộ chưa xác minh/nhiều căn hộ, snapshot và chuyển căn, IDOR trên detail/history/list, role và chung cư, membership hết hạn/bị kết thúc, tài khoản bị khóa, giả trường, request sai/ngoài phạm vi, CSRF, phân trang, replay/cạnh tranh và rollback khi ghi lịch sử thất bại. Test trình duyệt đi qua form, mất response sau commit rồi retry, chi tiết, quản lý và cư dân khác bị từ chối.

Kết quả local ngày 09/11/2026:

| Kiểm tra | Kết quả |
|---|---|
| Maven `verify` | 89 tests (63 hồi quy M2 + 26 M3), 0 failures/errors/skipped; BUILD SUCCESS |
| Frontend lint / production build | Thành công, không có cảnh báo lint |
| Playwright với API/DB/Mailpit thật | 5/5 kịch bản thành công |
| Responsive | Form/chi tiết/danh sách cư dân ở 390px; danh sách quản lý ở 1440px và kiểm tra tràn ngang ở 390px/768px |

[Ảnh ứng dụng chạy thật](../design/m3-implemented/README.md).

M2 đã đóng bằng PR #12 và CI main xanh. M3 giữ issue #4 mở cho đến khi review, CI trên commit mới và merge hoàn tất. Nhánh local: `feat/m3-resident-issues`; chưa dùng CI M2 để chứng minh M3.
