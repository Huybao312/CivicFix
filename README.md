# CivicFix

CivicFix là dự án web responsive hỗ trợ cư dân báo sự cố kỹ thuật tại khu vực chung của chung cư, đồng thời giúp ban quản lý tiếp nhận, phân công và theo dõi quá trình xử lý.

**Trạng thái:** Dự án portfolio đang ở giai đoạn phân tích quy trình và thiết kế kịch bản mô phỏng. Repository hiện chứa tài liệu sản phẩm; ứng dụng chưa được triển khai. Nhu cầu người dùng thực tế chưa được xác thực.

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
- [Roadmap và milestone](docs/ROADMAP.md): thứ tự thực hiện, đầu ra của từng mốc, điều kiện hoàn thành và việc nên làm ngay.
- [Sổ giả định và kịch bản mô phỏng](docs/ASSUMPTIONS.md): phân biệt căn cứ công khai, suy luận và các điều chưa thể xác minh.

## Bước tiếp theo

1. Tổng hợp thông tin công khai về quy trình báo và xử lý sự cố; dựng các tình huống mô phỏng có nguồn và giả định rõ ràng.
2. Ghi lại những điều được tài liệu hỗ trợ và những giả định chưa thể xác minh.
3. Thiết kế luồng cốt lõi trên web responsive, sau đó mới triển khai từng mốc.

Đây là dự án cá nhân phục vụ học tập và portfolio. Những chính sách vận hành trong PRD chưa được một chung cư cụ thể xác nhận.

Các [milestone trên GitHub](https://github.com/Huybao312/CivicFix/milestones) được chia theo luồng chức năng. Mỗi issue chức năng chỉ hoàn thành khi giao diện, API, dữ liệu, phân quyền và kiểm thử liên quan cùng chạy được.
