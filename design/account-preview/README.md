# CivicFix — bản xem trước vòng đời tài khoản

Các màn hình này là **mockup tĩnh**, chưa phải giao diện hoạt động. Tất cả tên, email và căn hộ đều là dữ liệu giả. Bản SVG là nguồn vector; bản PNG thuận tiện để xem nhanh.

## Quản trị viên

1. [Danh sách tài khoản](01-admin-accounts.png) — tìm cư dân, thấy căn hộ và trạng thái.
2. [Mời cư dân](02-admin-invite.png) — nhập email, căn hộ, căn cứ xác minh; kiểm tra trước khi gửi.
3. [Lời mời đã gửi](03-admin-invite-sent.png) — xem trạng thái, gửi lại hoặc thu hồi.
4. [Chi tiết và kết thúc quyền](04-admin-end-access.png) — kết thúc membership khi cư dân chuyển đi.

## Cư dân chưa có tài khoản

1. [Kích hoạt và tự đặt mật khẩu](05-resident-activate.png).
2. [Báo căn hộ trong lời mời bị sai](06-resident-wrong-apartment.png).
3. [Kích hoạt thành công](07-resident-activated.png).
4. [Link đã hết hạn, đã dùng hoặc đã thu hồi](08-resident-invite-invalid.png).

## Cư dân đã có tài khoản và khôi phục

- [Chấp nhận thêm liên kết căn hộ bằng tài khoản hiện có](12-existing-user-accept.png) — đăng nhập, **không đổi mật khẩu**.
- [Đăng nhập](13-resident-login.png) → [thông báo đăng nhập không thành công](14-resident-login-error.png).
- [Quên mật khẩu](09-resident-forgot-password.png) → [đặt mật khẩu mới](10-resident-reset-password.png).
- [Quyền truy cập đã kết thúc](11-resident-access-ended.png).

Quy tắc quyền, trạng thái, token, email thử nghiệm và các ca lỗi nằm trong [đặc tả vòng đời tài khoản](../../docs/ACCOUNT_LIFECYCLE.md). Luồng này thuộc M2; chưa có frontend hay backend.

## Ghi chú cho vòng thiết kế tiếp theo

- Màn hình quản trị viên dùng desktop vì thao tác xác minh và kiểm soát quyền cần đọc nhiều thông tin. Kích hoạt/khôi phục cho cư dân ưu tiên điện thoại.
- Nút gửi lại và thu hồi chỉ áp dụng cho lời mời đang chờ; giao diện triển khai phải phản ánh trạng thái thực tế từ server.
- Màn hình mời không thể tự chứng minh người nhận ở căn hộ; quản trị viên phải có căn cứ ngoài ứng dụng.
- Figma hiện chỉ chứa các màn hình cư dân báo sự cố trước đó. Các màn hình tài khoản ở đây là bản xem trước local vì giới hạn ghi của Figma trong gói hiện tại.
