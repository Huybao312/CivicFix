# M4 — Giao diện tiếp nhận và phân công

Ảnh chụp từ ứng dụng React, Spring Boot và PostgreSQL đang chạy qua Playwright ngày 14/11/2026. Dùng tài khoản và tình huống giả lập của CivicFix.

- [Hộp thoại yêu cầu bổ sung](question-dialog.png)
- [Hộp thoại phân công kỹ thuật viên](assignment-dialog.png)
- [Hàng đợi quản lý với bộ lọc](manager-queue.png)
- [Cư dân xem câu hỏi trên điện thoại](resident-question-mobile.png)
- [Kỹ thuật viên xem việc được giao trên điện thoại](technician-mobile.png)

Modern Web Guidance được áp dụng vào native dialog, focus khi mở/đóng, xác thực form sau tương tác và thông báo lỗi cho công nghệ hỗ trợ. Playwright kiểm tra trực tiếp các hành vi này, cùng luồng nghiệp vụ M4.

Các bước chạy thử và API nằm trong [tài liệu M4](../../docs/M4_IMPLEMENTATION.md).
