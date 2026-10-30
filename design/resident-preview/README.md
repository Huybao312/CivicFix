# CivicFix — bản xem trước cho cư dân

Figma: [CivicFix — Màn hình cư dân v1](https://www.figma.com/design/BsCO12c8Solp1rQ0779koy)

Đây là **bản thiết kế tĩnh**, dùng để duyệt luồng và giao diện trước khi viết frontend. Nội dung phiếu, chung cư, căn hộ và ngày tháng là dữ liệu minh họa; không phải hồ sơ cư dân thật.

## Luồng chính

1. [Đăng nhập](01-login.png) → [Phiếu của tôi](02-my-tickets.png).
2. Chọn **Báo sự cố khu vực chung** → [xác định phạm vi](03-scope.png) → [nhập vị trí và mô tả](04-report-form.png).
3. Nếu có phiếu tương tự, [xem gợi ý trùng](05-duplicate.png), rồi theo dõi phiếu hiện có hoặc tiếp tục tạo phiếu riêng.
4. Sau khi gửi, [xem mã phiếu và trạng thái chờ xem xét](06-submitted.png).
5. Khi quản lý hỏi thêm, [trả lời ngay trong phiếu](07-needs-info.png).
6. Khi quản lý xác nhận đã khắc phục, [xem kết quả và báo “Vẫn còn lỗi” nếu cần](08-resolved-feedback.png). [Thông báo](09-notifications.png) dẫn về phiếu liên quan.

## Điện thoại

- [Phiếu của tôi](10-mobile-my-tickets.png)
- [Báo sự cố](11-mobile-report.png)
- [Trả lời yêu cầu bổ sung](12-mobile-detail.png)

## Quyết định thiết kế cần giữ khi triển khai

- Trang đầu trả lời: **phiếu nào cần cư dân làm gì tiếp theo**. Danh sách phiếu là phần chính; không dùng bốn thẻ KPI lớn hoặc thanh trạng thái lặp thông tin.
- Biểu mẫu chỉ bắt buộc **vị trí sự cố** và **mô tả ngắn**. Ảnh là tùy chọn. Trước khi gửi phải kiểm tra báo cáo tương tự.
- **Căn hộ người báo** và **vị trí sự cố** là hai trường khác nhau. Căn hộ của người khác không xuất hiện trong gợi ý trùng.
- “Đã ghi nhận” chỉ xác nhận hệ thống lưu phiếu; quản lý còn phải xem xét trước khi tiếp nhận xử lý.
- Phản hồi “Vẫn còn lỗi” gửi đến quản lý xác minh; không tự mở lại phiếu hoặc tự đổi trạng thái.
- Cảnh báo nguy hiểm chỉ dẫn đến số liên hệ **đã được ban quản lý cấu hình và xác nhận**. Thiết kế chưa tự đặt số điện thoại.

## Giới hạn của bản v1

- Figma gồm các frame chỉnh sửa được; hiện chưa nối prototype click-through.
- Bản điện thoại minh họa ba màn hình thường dùng nhất; các trạng thái còn lại đã có bản desktop.
- Các phiếu minh họa trên từng màn hình dùng để thử cách đọc và mật độ thông tin, chưa phải một bộ dữ liệu xuyên suốt đã chốt.
