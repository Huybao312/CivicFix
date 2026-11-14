# M4 — Tiếp nhận, hỏi bổ sung và phân công

M4 bổ sung hàng đợi cho quản lý, trao đổi thông tin với người báo và trang Việc của tôi cho kỹ thuật viên. Dùng các tài khoản demo và cách khởi động trong README. Migration V8 áp dụng trên dữ liệu M3 hiện có.

## Các luồng

| Thao tác | Trạng thái trước | Trạng thái sau | Người thực hiện |
|---|---|---|---|
| Tiếp nhận | SUBMITTED | ACCEPTED | Quản lý, có lý do công khai |
| Hỏi bổ sung | SUBMITTED | NEEDS_INFO | Quản lý, có câu hỏi |
| Trả lời | NEEDS_INFO | SUBMITTED | Chính người báo, có nội dung mới |
| Từ chối | SUBMITTED, NEEDS_INFO, ACCEPTED | REJECTED | Quản lý, có lý do công khai |
| Khôi phục | REJECTED | SUBMITTED | Quản lý, có lý do công khai |
| Đổi ưu tiên | Trạng thái đang hoạt động | Giữ trạng thái | Quản lý, có lý do nội bộ |
| Phân công | ACCEPTED | ASSIGNED | Quản lý, chọn kỹ thuật viên hợp lệ và ghi lý do công khai |

Mức ưu tiên LOW, MEDIUM, HIGH, URGENT theo PRD, mặc định MEDIUM. Hàng đợi quản lý xếp phiếu chờ xem xét trước, tiếp theo cần thông tin, đã tiếp nhận và đã phân công. Trong từng nhóm, mức ưu tiên cao hơn đứng trước. Bộ lọc trạng thái, ưu tiên và tìm kiếm áp dụng cùng nhau khi chọn Tìm kiếm.

Câu hỏi và câu trả lời được ghi thành event mới. Mô tả ban đầu và snapshot căn hộ giữ nguyên. Lịch sử lưu quyết định từ chối và khôi phục. Lý do đổi ưu tiên là event INTERNAL, hiển thị cho quản lý. Các quyết định khác dùng nội dung công khai, được ghi rõ trong nhãn nhập.

## API

Các endpoint yêu cầu session hợp lệ và kiểm tra quyền ở backend. POST cần CSRF token như M2.

| Method | Endpoint | Nội dung |
|---|---|---|
| GET | `/api/buildings/{building}/issues?status=&priority=&q=&page=0&size=20` | Danh sách theo quyền, bộ lọc và phân trang |
| GET | `/api/buildings/{building}/issues/technicians` | Kỹ thuật viên hoạt động ở đúng chung cư, dành cho quản lý |
| POST | `/api/buildings/{building}/issues/{id}/actions/{action}` | Thực hiện quyết định, cần header Idempotency-Key UUID |
| GET | `/api/buildings/{building}/notifications` | Tối đa 100 thông báo mới nhất của chính người đang đăng nhập, lọc quyền phiếu hiện tại |
| POST | `/api/buildings/{building}/notifications/{id}/read` | Ghi read_at khi người nhận mở phiếu từ thông báo |

Command dùng các action ACCEPT, REQUEST_INFO, PROVIDE_INFO, REJECT, RESTORE, SET_PRIORITY, ASSIGN.

```json
{
  "expectedVersion": 2,
  "text": "Đủ thông tin để kiểm tra thiết bị khu vực chung."
}
```

Mọi command cần `expectedVersion` và nội dung trim dài 3–800 ký tự. SET_PRIORITY thêm `priority`. ASSIGN thêm `technicianId`. Trường ngoài hợp đồng bị từ chối. Backend trả chi tiết phiếu cập nhật, gồm `permissions.manage` và `permissions.provideInfo` để giao diện hiển thị thao tác phù hợp.

Sai phiên bản trả 409 với `message`, `currentVersion`, `currentStatus`. Người dùng tải lại, xem trạng thái mới và xác nhận lại quyết định. Giao diện giữ nội dung đã nhập. Khi kết quả mạng chưa xác định, gửi lại dùng đúng payload và key cũ.

## Quyền và tính nhất quán

- Manager có membership hiệu lực tại chung cư được quyết định trên phiếu của chung cư đó.
- Người báo có quyền nghiệp vụ hiệu lực được trả lời phiếu của mình. Người quản lý nhận thông tin từ người ngoài web sẽ dùng luồng nhập hộ ở M6.
- Technician cần role đúng chung cư và assignment còn mở của chính mình để đọc phiếu. API loại các trường reported_by, reporter_name_at_report, apartment_id_at_report và apartment_label_at_report khỏi projection của kỹ thuật viên. Quyền nhiều role được hợp theo ma trận quyền.
- Khi assignment hoặc membership mất hiệu lực, API detail/history/list và thông báo kiểm tra lại quyền hiện tại.
- Event INTERNAL chỉ được trả cho quản lý. Lịch sử quản lý có actor_id và actor_name. Cư dân và kỹ thuật viên nhận các event công khai.
- Command khóa các user liên quan theo thứ tự xác định, sau đó khóa phiếu. Kiểm tra role được lặp lại sau khi lấy khóa để phối hợp với thao tác thu hồi tài khoản M2.
- Phiên bản được so sánh trong khóa phiếu. Hai quản lý phân công cùng phiên bản có một request thành công và một request nhận 409.
- Unique partial index bảo đảm mỗi phiếu có tối đa một assignment chưa kết thúc.
- Status, priority, question, assignment, event, notification và command receipt được ghi trong cùng transaction. Lỗi ở bất kỳ bước ghi nào làm rollback toàn bộ.
- Idempotency receipt ràng buộc actor, phiếu và key. Retry cùng nội dung trả lại phiếu hiện tại. Key cũ với nội dung khác nhận 409. Quyền hiện tại vẫn được kiểm tra trước replay.

## Thông báo

Yêu cầu bổ sung, tiếp nhận, từ chối và khôi phục tạo thông báo cho người báo. Phân công tạo thông báo cho người báo và kỹ thuật viên nhận việc. Trả lời câu hỏi tạo thông báo cho quản lý đang có quyền tại chung cư. Mỗi event có tối đa một thông báo cho mỗi người nhận.

Mở trang danh sách và đăng nhập giữ thông báo ở trạng thái chưa đọc. Mở phiếu từ liên kết thông báo thành công mới gọi API ghi read_at. Endpoint dùng COALESCE để retry giữ lại thời điểm đọc đầu tiên. Thông báo dùng nội dung giới hạn và được kiểm tra quyền đối tượng khi đọc.

## Modern Web Guidance áp dụng

Đã tra và đọc guide dialog, forms, accessible-error-announcement và validate-input-after-interaction bằng CLI.

- Hộp thoại dùng native dialog/showModal, có tên truy cập duy nhất và hỗ trợ Escape. Focus được chuyển vào trường được chỉ định. Đóng bằng native close trả focus về nút mở. Sau quyết định làm nút cũ biến mất, focus chuyển đến tiêu đề phiếu cập nhật.
- FormField đặt thuộc tính autofocus thực trên DOM để trình duyệt thấy nó khi gọi showModal. React autoFocus hỗ trợ trường kỹ thuật viên được thêm sau khi tải dữ liệu.
- Form dùng required, minlength, maxlength và custom validity cho nội dung chỉ có khoảng trắng. Lỗi xuất hiện sau tương tác hoặc submit, kèm nội dung sửa lỗi và aria-invalid.
- aria-describedby chỉ nối thông báo lỗi khi lỗi hiển thị. Một vùng aria-live polite thông báo sau blur, được dọn khi lỗi đã sửa.
- Bộ tìm kiếm và lọc nằm trong search element, có nút submit rõ ràng.

## Kiểm chứng

Chạy `./mvnw --batch-mode verify` trong backend. Chạy `npm run lint`, `npm run build`, `npm run test:e2e` trong frontend. E2E dùng môi trường riêng, tài khoản giả và dữ liệu thử.

Nhóm TriageTests có 28 ca kiểm tra các chuyển trạng thái, idempotency, xung đột phiên bản, hai quản lý phân công đồng thời, kỹ thuật viên không hợp lệ, quyền đối tượng, thông báo và rollback khi ghi event/assignment/notification thất bại. Hồi quy M3 được cập nhật để kỹ thuật viên được mở danh sách việc rỗng trước khi có phân công.

Luồng trình duyệt kiểm tra focus, Escape, lỗi trường nhập, xung đột phiên bản, thông báo chưa đọc/đã đọc, hỏi–đáp, từ chối/khôi phục, ưu tiên và phân công.

[Ảnh giao diện chạy thật](../design/m4-implemented/README.md).

Các ca bổ sung kiểm tra membership hết hạn trong khi command chờ khóa phiếu và tài khoản nhiều role đọc assignment của đúng đối tượng. Kiểm tra thời hạn dùng thời điểm câu lệnh SQL, sau khi lấy khóa thì xác thực quyền lần nữa.

Code M4 đang ở nhánh feat/m4-triage-assignment. Kết quả local ngày 14/11/2026 gồm 117 backend tests, 0 failures/errors/skipped và BUILD SUCCESS. Frontend lint và production build thành công. Cả 6 kịch bản Playwright với API, PostgreSQL và Mailpit thật đều đạt.

Các thay đổi đang ở working tree của nhánh M4. CI trên GitHub sẽ chạy khi commit/push nhánh này.
