# Sổ giả định và kịch bản mô phỏng — CivicFix

**Trạng thái:** Tài liệu làm việc cho portfolio. Chưa có phỏng vấn cư dân hoặc ban quản lý. Những tình huống dưới đây là giả lập, không phải sự kiện đã quan sát.

## Cách ghi

Với mỗi nhận định, dùng một trong ba nhãn:

- **Có căn cứ công khai:** Có tài liệu hoặc ví dụ được dẫn nguồn; phải ghi rõ bối cảnh của nguồn. Nguồn từ nơi khác không chứng minh tình hình tại một chung cư cụ thể.
- **Suy luận:** Quyết định thiết kế được rút ra từ bài toán hoặc ràng buộc kỹ thuật, chưa có người dùng xác nhận.
- **Chưa xác minh:** Chưa có bằng chứng đủ để quyết định cho môi trường thật.

Khi tìm tài liệu công khai, lưu URL, ngày truy cập, ai công bố và câu nào trong tài liệu hỗ trợ nhận định. Không đưa ảnh chụp, tên hoặc thông tin cá nhân của cư dân thật vào repository.

## Giả định cần theo dõi

| ID | Giả định | Nhãn hiện tại | Nếu sai thì sao? | Cách kiểm chứng khi có cơ hội |
|---|---|---|---|---|
| A1 | Báo cáo sự cố đến từ nhiều kênh và cần nhập về một nơi | Chưa xác minh | Màn nhập hộ có thể không cần hoặc chưa đủ | Quan sát một ca tiếp nhận thật; hỏi người quản lý về kênh và số lần nhập lại |
| A2 | Một người trong ban quản lý có thể chịu trách nhiệm nhập và cập nhật phiếu | Chưa xác minh | Dữ liệu CivicFix sẽ không đầy đủ, dashboard gây hiểu sai | Xác nhận người phụ trách và thử thao tác nhập hộ với họ |
| A3 | Cư dân chấp nhận báo qua web khi form ngắn và tiến độ rõ | Chưa xác minh | Cư dân vẫn dùng chat/điện thoại; lượng phiếu từ web thấp | Quan sát cư dân dùng prototype và so sánh với kênh họ đang chọn |
| A4 | Kỹ thuật viên có thể ghi cập nhật ngắn trên điện thoại tại hiện trường | Chưa xác minh | Nhật ký bị bỏ trống hoặc điền sau, mốc thời gian thiếu tin cậy | Thử tác vụ trên thiết bị và điều kiện kết nối họ thực sự dùng |
| A5 | Quản lý có thể quyết định lúc nào hai báo cáo thuộc cùng một sự cố | Suy luận | Gộp sai làm mất một công việc cần sửa riêng | Cho người tiếp nhận phân loại các cặp báo cáo thật/giả và giải thích quyết định |
| A6 | Người báo muốn phản hồi khi hệ thống ghi “đã khắc phục” | Chưa xác minh | Chức năng phản hồi có thể ít dùng hoặc gây vòng xử lý dài | Quan sát ca sửa chưa triệt để; thử giao diện phản hồi với cư dân |

## Nhật ký nguồn công khai

| Nguồn/URL | Ai công bố, bối cảnh | Thông tin dùng được | Điều không thể suy ra |
|---|---|---|---|
| Chưa thêm | — | — | — |

## Ba kịch bản bắt đầu

Các tình huống này dùng để thiết kế và kiểm thử luồng, không đại diện cho tần suất xảy ra ngoài đời.

### S1 — Hai người cùng báo đèn hành lang hỏng

Hai tài khoản demo gửi báo cáo tại cùng vị trí. Quản lý xem ảnh/mô tả, quyết định gộp; cả hai người theo dõi được tiến độ công khai. Cần kiểm tra quyền riêng tư và thao tác tách gộp nếu quyết định sai.

### S2 — Quản lý nhập hộ cuộc gọi báo rò nước

Một người gọi điện nhưng chưa có tài khoản. Quản lý tạo phiếu với nguồn `PHONE`, thời điểm nhận cuộc gọi và thời điểm nhập hệ thống riêng. Cần kiểm tra ai được xem phiếu và cách không tạo danh tính giả cho người gọi.

### S3 — Báo đã sửa nhưng lỗi vẫn còn

Kỹ thuật viên đề xuất hoàn thành, quản lý xác nhận, người báo phản hồi vẫn còn lỗi. Quản lý quyết định mở lại cùng phiếu; các mốc giải quyết cũ vẫn còn trong lịch sử. Cần phân biệt lỗi chưa sửa triệt để với hỏng hóc mới.

## Câu hỏi mở trước pilot thật

1. Ai theo dõi các kênh tiếp nhận và nhập hộ báo cáo? Vào thời gian nào?
2. Kênh nào dùng cho nguy hiểm tức thời và ngoài giờ?
3. Ai được phép gộp, từ chối, tạm hoãn, xác nhận đã sửa và đóng phiếu?
4. Cư dân được xem thông tin nào của một sự cố do người khác báo?
5. Mục tiêu thời gian và thời gian lưu dữ liệu do ai phê duyệt?

Không cần trả lời các câu hỏi này bằng cách bịa ra “chính sách chung cư”. Với bản portfolio, ghi lựa chọn mô phỏng trong PRD và dữ liệu demo; với pilot thật, phải xác nhận lại cùng đơn vị vận hành.
