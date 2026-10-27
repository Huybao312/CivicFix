# Ma trận quyền và dữ liệu CivicFix — quyết định cho bản demo

**Trạng thái:** Quy tắc triển khai cho portfolio, chưa phải chính sách của một chung cư thật. Chỉ áp dụng cho sự cố khu vực chung. Khi có pilot, người vận hành phải duyệt lại từng quyền liên quan đến dữ liệu cá nhân.

## 1. Nguyên tắc quyết định quyền

Một request chỉ hợp lệ khi **tài khoản đang hoạt động**, có membership còn hiệu lực ở **đúng chung cư**, được phép thực hiện **hành động** đó, được phép thao tác trên **phiếu cụ thể**, và phiếu đang ở **trạng thái phù hợp**. Backend kiểm tra đủ năm điều kiện trên từng API và từng tệp. Quyền từ nút trên giao diện không có giá trị bảo vệ API.

Vai trò được cấp trong `building_memberships`; một người có nhiều vai trò thì quyền là hợp của những vai trò **đang hiệu lực tại cùng chung cư**. Quản trị viên không tự có quyền quản lý phiếu. Người quản lý ở chung cư A không có quyền với phiếu chung cư B, kể cả đoán đúng ID.

Ký hiệu trong bảng: **Có** = được phép trong phạm vi vai trò; **Của mình** = người tạo/người báo đã xác minh trên phiếu; **Được giao** = kỹ thuật viên chính đang còn phân công; **Bản công khai** = dữ liệu giới hạn của hồ sơ chính khi theo dõi báo trùng; **Không** = từ chối ở backend.

## 2. Ma trận hành động

| Hành động | Cư dân | Quản lý | Kỹ thuật viên | Quản trị viên |
|---|---|---|---|---|
| Tạo phiếu qua web | Có, với danh tính của chính mình | Có, với danh tính của chính mình | Chỉ khi cũng có role cư dân | Chỉ khi cũng có role cư dân |
| Nhập hộ phiếu từ điện thoại/chat/trực tiếp | Không | Có | Không | Không, trừ khi cũng có role quản lý |
| Xem danh sách và chi tiết phiếu | Của mình; bản công khai của phiếu chính đang theo dõi | Mọi phiếu cùng chung cư | Phiếu được giao | Không, trừ khi có role khác |
| Bổ sung sau yêu cầu thông tin | Của mình | Có thể ghi thay lời người báo ngoài web, phải nêu nguồn | Không | Không |
| Tiếp nhận, hỏi thêm, từ chối, khôi phục | Không | Có | Không | Không |
| Gộp/tách báo trùng và đặt ưu tiên | Không | Có | Không | Không |
| Phân công, thu hồi, chuyển người, xác nhận tạm hoãn | Không | Có | Không | Không |
| Bắt đầu, ghi công việc, đề xuất hoàn thành | Không | Ghi chú điều phối riêng; không giả mạo cập nhật kỹ thuật | Được giao | Không |
| Xác nhận RESOLVED, đóng, mở lại | Phản hồi “vẫn lỗi” trên phiếu mình | Có | Không | Không |
| Ghi lần liên hệ người báo ngoài web | Không | Có | Không | Không |
| Quản lý tài khoản, role, liên kết căn hộ xác minh | Không | Không trong bản demo | Không | Có; có nhật ký thay đổi |

Nhập hộ luôn lưu **người nhập** và **nguồn nhận báo**; không biến người quản lý thành người báo và không tạo tài khoản cư dân giả. Một người ngoài web chỉ trở thành người báo có tài khoản khi danh tính được xác minh và liên kết có căn cứ.

## 3. Ma trận dữ liệu đọc được

| Loại dữ liệu | Cư dân trên phiếu mình | Cư dân theo dõi phiếu chính do báo trùng | Quản lý đúng chung cư | Kỹ thuật viên đang được giao | Quản trị viên chỉ có role quản trị |
|---|---|---|---|---|---|
| Mã phiếu, mô tả, vị trí, trạng thái, kết quả của phiếu mình | Có | Chỉ tiêu đề/vị trí đã lọc và tiến độ công khai của hồ sơ chính | Có | Có | Không |
| Tên, số căn hộ đã xác minh, thông tin liên hệ người báo | Của chính mình | Không | Có, căn hộ tự khai có nhãn “chưa xác minh” | Không mặc định; bản demo không trả các trường này | Chỉ trong màn quản lý danh tính, không qua phiếu |
| Ảnh do người báo gửi làm bằng chứng | Có trên phiếu mình | Không | Có | Có trên phiếu được giao để chẩn đoán | Không |
| Ảnh công việc do kỹ thuật viên tải lên | Chỉ ảnh kết quả được quản lý đánh dấu công khai trên phiếu mình | Không trong bản demo | Có | Có trên phiếu được giao | Không |
| Nhật ký trạng thái công khai và lý do hiển thị cho cư dân | Có | Chỉ các mốc công khai của hồ sơ chính | Có | Có trên phiếu được giao | Không |
| Ghi chú nội bộ, lịch sử phân công chi tiết, kết quả liên hệ | Không | Không | Có | Chỉ ghi chú công việc liên quan đến mình; không có thông tin liên hệ | Không |
| Thông báo | Chỉ thông báo gửi cho mình | Chỉ thông báo gửi cho mình, nội dung giới hạn | Chỉ thông báo gửi cho mình | Chỉ thông báo gửi cho mình | Chỉ thông báo gửi cho mình |

Ảnh **không mặc nhiên công khai** khi gộp phiếu. Mọi lần tải ảnh phải kiểm tra quyền đối tượng ở backend; storage riêng tư, không trả URL lâu dài. Khi kỹ thuật viên bị thu hồi việc, quyền đọc ảnh và ghi cập nhật của phiếu đó hết ngay. Nội dung ảnh có thể chứa dữ liệu cá nhân; bản demo chỉ dùng ảnh giả hoặc ảnh đã làm sạch.

## 4. Phạm vi phiếu và lịch sử

- Cư dân xem phiếu mình đã báo trong thời gian tài khoản/membership hợp lệ; việc đổi căn hộ sau khi báo **không đổi** snapshot `apartment_id_at_report` của phiếu cũ. Tài khoản bị khóa hoặc rời chung cư không được dùng quyền cũ để đọc.
- Người theo dõi phiếu trùng chỉ đọc **bản công khai do server dựng**. Không gửi toàn bộ đối tượng phiếu chính rồi ẩn trường ở frontend. Không lộ tên, căn hộ, số điện thoại, ảnh hoặc ghi chú nội bộ qua danh sách, chi tiết, tìm kiếm, thông báo hay tệp.
- Kỹ thuật viên chỉ xem phiếu còn được giao. Khi cần liên hệ cư dân, ban quản lý điều phối; bản demo chưa cấp quyền xem số điện thoại/căn hộ cho kỹ thuật viên.
- Lịch sử bất biến có hai phần: mốc và diễn giải **công khai** cho người báo, dữ liệu điều phối/liên hệ **nội bộ** cho quản lý. Kỹ thuật viên thấy phần liên quan đến công việc được giao, không thấy toàn bộ nhật ký liên hệ.
- ID phiếu, event, ảnh và thông báo không được xem như bí mật. Với đối tượng không thuộc phạm vi đọc, API trả cùng kiểu `404` như không tồn tại để tránh dò ID; với hành động role không được cấp trên một đối tượng đã được phép đọc, trả `403`. Chuyển trạng thái sai hoặc version cũ trả `409`; dữ liệu thiếu/sai trả `422`.

## 5. Kiểm tra tối thiểu trước khi đóng từng chức năng

1. Đổi ID phiếu, ảnh, event và thông báo sang của cư dân khác hoặc chung cư khác: không lấy được dữ liệu.
2. Dùng request trực tiếp để giả `reported_by`, `building_id`, `apartment_id_at_report`, `status` hoặc `assigned_to`: server bỏ qua hoặc từ chối, không tin giá trị client.
3. Thu hồi membership hoặc phân công rồi gọi lại API: quyền cũ chấm dứt.
4. Người theo dõi phiếu trùng đọc mọi endpoint liên quan: chỉ nhận bản công khai, kể cả ảnh và thông báo.
5. Quản trị viên chỉ có role quản trị không thể đọc hồ sơ sự cố; quản lý không thể sửa tài khoản nếu thiếu role quản trị.
