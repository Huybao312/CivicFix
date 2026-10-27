# State machine và tiêu chí chấp nhận M1

**Trạng thái:** Hợp đồng hành vi cho bản portfolio; dữ liệu, tài khoản và chính sách vận hành là mô phỏng. Tài liệu này cùng [sơ đồ luồng](FLOWS.md) và [ma trận quyền](ACCESS_CONTROL.md) là đầu vào để viết issue, API và test từ M2. Không có số giờ SLA hoặc quy định của một chung cư thật trong tài liệu.

## 1. Quy ước bất biến

- Mọi command kiểm tra role, membership còn hiệu lực, building, quyền với phiếu, trạng thái hiện tại và version. Client không gửi trạng thái đích để server gán tùy ý.
- Chuyển trạng thái, sự kiện lịch sử và tác dụng phụ liên quan được ghi nhất quán. Retry cùng idempotency key không tạo thêm phiếu, sự kiện, thông báo, phân công hoặc việc liên hệ.
- `reported_by` và căn hộ đã xác minh lấy từ server. `apartment_id_at_report` là snapshot của **người báo**; `location_description` là **nơi hỏng**, không suy ra lẫn nhau.
- `REOPENED`, `DUPLICATE` và `PROPOSED_RESOLUTION` không phải status của phiếu. Chúng lần lượt là sự kiện mở lại, kết quả đóng phiếu phụ, và đề xuất của kỹ thuật viên.
- Lịch sử không bị xóa/ghi đè bởi lần giải quyết hoặc mở lại sau. Dữ liệu nhập hồi tố như `received_at` lưu riêng mốc nhập hệ thống và người nhập.

## 2. State machine có người chịu trách nhiệm

| Hành động | Trạng thái trước → sau | Người được phép | Điều kiện bắt buộc và tác dụng phụ |
|---|---|---|---|
| Tạo phiếu web hoặc nhập hộ | — → `SUBMITTED` | Cư dân của mình / quản lý nhập hộ | Đúng chung cư, thuộc phạm vi khu vực chung; lưu phiếu + sự kiện đầu tiên, nguồn và mốc nhận. |
| Yêu cầu bổ sung | `SUBMITTED` → `NEEDS_INFO` | Quản lý | Có câu hỏi cụ thể; người báo có tài khoản nhận thông báo, người ngoài web có việc theo dõi liên hệ phù hợp. |
| Bổ sung thông tin | `NEEDS_INFO` → `SUBMITTED` | Người báo đã liên kết / quản lý ghi nhận từ kênh ngoài | Có nội dung mới; ghi nguồn và actor, không sửa âm thầm báo cáo gốc. |
| Tiếp nhận | `SUBMITTED` → `ACCEPTED` | Quản lý | Đã kiểm tra phạm vi và đủ dữ liệu; ghi người quyết định. |
| Từ chối | `SUBMITTED`, `NEEDS_INFO`, hoặc `ACCEPTED` → `REJECTED` | Quản lý | Có lý do công khai dễ hiểu; với `ACCEPTED` chỉ dùng khi phát hiện ngoài phạm vi hoặc sai căn cứ. Không tự từ chối theo đồng hồ. |
| Khôi phục từ chối | `REJECTED` → `SUBMITTED` | Quản lý | Có lý do và sự kiện khôi phục. |
| Gộp báo trùng | `SUBMITTED` → `CLOSED` | Quản lý | Chỉ khi cùng một sự cố vật lý; có phiếu chính hợp lệ, lý do, kết quả `DUPLICATE`, không tạo vòng; người báo phiếu phụ theo dõi bản công khai. |
| Tách gộp sai | Phiếu phụ `CLOSED`/`DUPLICATE` → `SUBMITTED` | Quản lý | Có lý do, xóa quan hệ hiện hành nhưng giữ sự kiện gộp/tách cũ; phiếu phụ xử lý độc lập. |
| Giao việc | `ACCEPTED` → `ASSIGNED` | Quản lý | Một kỹ thuật viên đang hoạt động, có role đúng chung cư; một phân công chính hiện hành. |
| Thu hồi | `ASSIGNED` → `ACCEPTED` | Quản lý | Có lý do; người cũ mất quyền. |
| Bắt đầu | `ASSIGNED` → `IN_PROGRESS` | Kỹ thuật viên đang được giao | Ghi sự kiện bắt đầu. |
| Đổi người | `ASSIGNED` hoặc `IN_PROGRESS` → `ASSIGNED` | Quản lý | Kết thúc phân công cũ, tạo phân công mới cùng transaction; nêu lý do, không có hai người chính. |
| Tạm hoãn | `ACCEPTED`, `ASSIGNED`, `IN_PROGRESS` → `ON_HOLD` | Quản lý sau đề xuất hoặc tự quyết định | Có lý do và mốc cập nhật tiếp; lưu trạng thái trước khi chờ. |
| Tiếp tục | `ON_HOLD` → trạng thái trước đó, hoặc `ASSIGNED` khi đổi người | Quản lý | Kết thúc khoảng chờ; nếu đổi người, phân công mới hợp lệ. |
| Đề xuất hoàn thành | `IN_PROGRESS` → `IN_PROGRESS` | Kỹ thuật viên đang được giao | Có mô tả việc đã làm và kết quả kiểm tra; tạo event đề xuất, chưa báo cư dân “đã khắc phục”. |
| Xác nhận kết quả | `IN_PROGRESS` → `RESOLVED` | Quản lý | Có đề xuất hợp lệ và mô tả kết quả; tạo đúng một thông báo/người có tài khoản và một việc liên hệ/người ngoài web cần báo kết quả. |
| Phản hồi vẫn còn lỗi | `RESOLVED` hoặc `CLOSED` không mang kết quả `DUPLICATE` → giữ nguyên status | Người báo của phiếu / quản lý ghi từ kênh ngoài | Có mô tả; tạo phản hồi để quản lý xác minh, không tự kết luận kỹ thuật viên sai. Người báo phiếu trùng phản hồi qua hồ sơ chính. |
| Mở lại cùng lỗi | `RESOLVED` hoặc `CLOSED` → `ASSIGNED` hoặc `IN_PROGRESS` | Quản lý | Có lý do xác minh và người chịu trách nhiệm hợp lệ; tăng `reopen_count`, giữ mọi mốc cũ. |
| Đóng phiếu đã giải quyết | `RESOLVED` → `CLOSED` | Quản lý | Đã kiểm tra kết quả; phiếu nhập hộ có ít nhất một lần liên hệ được ghi hoặc lý do không thể liên hệ. Không tự đóng sau thời gian. |

Kỹ thuật viên có thể tiếp tục ghi cập nhật ngắn trong `IN_PROGRESS` mà không đổi status. Nếu quản lý không chấp nhận đề xuất hoàn thành, phiếu ở `IN_PROGRESS` và sự kiện từ chối đề xuất phải có lý do. Hỏng hóc **mới** sau lần sửa là phiếu mới liên kết “tái diễn”; không dùng mở lại để che số lần phát sinh.

## 3. Tiêu chí chấp nhận có thể viết thành test

Mỗi dòng là một ca độc lập; “kết quả” phải quan sát được qua UI/API **và** dữ liệu/sự kiện tương ứng. Test quyền và trạng thái nằm trong issue triển khai chức năng, không dồn đến cuối dự án.

| ID | Luồng / điều kiện ban đầu | Khi thực hiện | Kết quả cần kiểm chứng |
|---|---|---|---|
| AC-01 | Tài khoản cư dân đang hoạt động, có căn 03 được xác minh | Báo đèn hỏng ở hành lang với vị trí và mô tả | Nhận một mã phiếu `SUBMITTED`; phiếu + event đầu tiên cùng transaction; snapshot căn 03 thuộc người báo, vị trí là hành lang. |
| AC-02 | Cư dân chưa có căn hộ xác minh | Gửi báo cáo khu vực chung hợp lệ | Phiếu vẫn được tạo; quản lý thấy “căn hộ chưa xác minh”, không lấy căn hộ từ chuỗi vị trí. |
| AC-03 | Cư dân có nhiều căn hộ xác minh | Chọn căn hộ khác danh sách của mình hoặc gửi `reported_by` giả | Server từ chối giá trị ngoài quyền; danh tính người báo luôn lấy từ session. |
| AC-04 | Cư dân chọn thiết bị riêng trong căn hộ | Cố gửi dưới loại sự cố khu vực chung | Form hướng dẫn kênh phù hợp; API không tạo phiếu khu vực chung khi loại ngoài phạm vi đã được xác định. |
| AC-05 | Có nguy hiểm tức thời | Mở form báo sự cố | Hướng dẫn gọi đầu mối đã xác nhận hiển thị trước form; không trình bày phiếu web như kênh ứng cứu tức thời. |
| AC-06 | Cùng một request được gửi lại do mạng hoặc bấm lặp | Gửi hai lần với cùng idempotency key | Trả cùng kết quả; DB có một phiếu và một event tạo phiếu. |
| AC-07 | Cư dân khác hoặc nhân viên khác chung cư | Đổi ID trong phiếu, ảnh, event, thông báo | Không đọc được nội dung hoặc sự tồn tại của đối tượng; không có URL ảnh công khai. |
| AC-08 | Phiếu `SUBMITTED` thiếu vị trí cụ thể | Quản lý hỏi bổ sung | `NEEDS_INFO` có câu hỏi và event; người báo có tài khoản thấy thông báo. Khi người báo trả lời, phiếu về `SUBMITTED` với lịch sử hai chiều. |
| AC-09 | Phiếu `NEEDS_INFO` quá khoảng chờ mô phỏng | Không ai thao tác hoặc quản lý từ chối | Hệ thống không tự từ chối; nếu quản lý từ chối phải có lý do. |
| AC-10 | Phiếu ngoài phạm vi | Quản lý từ chối rồi khôi phục do nhận định sai | `REJECTED` có lý do; khôi phục về `SUBMITTED` có event riêng, không xóa quyết định cũ. |
| AC-11 | Phiếu `SUBMITTED` hợp lệ | Quản lý tiếp nhận, đặt ưu tiên và giao kỹ thuật viên đúng chung cư | Trạng thái lần lượt `ACCEPTED`, `ASSIGNED`; một phân công chính; kỹ thuật viên thấy việc của mình. |
| AC-12 | Kỹ thuật viên sai chung cư, đã khóa hoặc không có role kỹ thuật | Quản lý cố phân công | Server từ chối, không đổi status hay tạo assignment. |
| AC-13 | Kỹ thuật viên không được giao hoặc đã bị thu hồi | Đọc/cập nhật phiếu bằng API cũ | Không đọc hoặc sửa được; kỹ thuật viên mới có quyền sau khi phân công commit. |
| AC-14 | Phiếu `IN_PROGRESS` | Kỹ thuật viên đề xuất hoàn thành không có mô tả/kết quả | Server từ chối; không tạo đề xuất hợp lệ, không chuyển `RESOLVED`. |
| AC-15 | Phiếu `IN_PROGRESS` có đề xuất hoàn thành hợp lệ | Quản lý xác nhận | `RESOLVED` có kết quả công khai dễ hiểu; tạo một thông báo cho mỗi người báo/người theo dõi có tài khoản hoặc việc liên hệ phù hợp. |
| AC-16 | Có thông báo kết quả chưa mở | Cư dân đăng nhập nhưng chưa mở thông báo, sau đó mở | Trước khi mở `read_at` rỗng; chỉ sau khi mở mới ghi `read_at`. |
| AC-17 | Phiếu `RESOLVED` hoặc `CLOSED` | Người báo phản hồi “vẫn lỗi”; quản lý xác minh cùng nguyên nhân và mở lại | Có feedback + quyết định; phiếu về `ASSIGNED`/`IN_PROGRESS`, `reopen_count` tăng; mốc giải quyết cũ còn nguyên trong lịch sử. |
| AC-18 | Phản hồi mô tả một hỏng hóc mới | Quản lý đánh giá | Tạo phiếu mới liên kết tái diễn; không mở lại phiếu cũ chỉ để dùng chung mã. |
| AC-19 | Hai người báo cùng một đèn hỏng | Quản lý gộp phiếu phụ vào phiếu chính | Phiếu phụ `CLOSED`/`DUPLICATE`, có lý do và link; người báo phụ thấy bản công khai, không thấy tên/căn hộ/ảnh/ghi chú của người báo chính. |
| AC-20 | Hai phiếu cùng tầng nhưng hai thiết bị khác nhau; hoặc chọn phiếu chính tạo vòng liên kết | Quản lý xem gợi ý và quyết định; thử gửi link tạo vòng | Gợi ý không tự gộp theo tầng/danh mục; API chặn vòng liên kết. Nếu quản lý gộp nhầm do đánh giá sai, tách được và giữ lịch sử gộp/tách. |
| AC-21 | Quản lý nhập hộ cuộc gọi | Nhập nguồn `PHONE`, thời điểm nhận trước thời điểm nhập, người gọi tự khai căn 03 | Lưu `received_at` và `created_at` riêng; lưu người nhập; căn 03 gắn nhãn chưa xác minh, không tạo user giả. |
| AC-22 | Phiếu nhập hộ chưa liên kết tài khoản được xác nhận `RESOLVED` | Hệ thống tạo việc liên hệ và quản lý ghi đã thử nhưng chưa gặp | Có contact task + attempt với actor/kênh/mốc/kết quả; không hiển thị “người báo đã nhận”. |
| AC-23 | Phiếu nhập hộ `RESOLVED`, chưa có attempt hay lý do thiếu kênh | Quản lý cố đóng | Server từ chối. Sau khi ghi attempt hoặc lý do không thể liên hệ, quản lý mới có thể đóng. |
| AC-24 | Phiếu `ASSIGNED`/`IN_PROGRESS` đang chờ vật tư | Quản lý xác nhận tạm hoãn rồi cho tiếp tục | `ON_HOLD` có lý do và mốc cập nhật; tiếp tục đúng trạng thái trước hoặc `ASSIGNED` khi đổi người; lịch sử thời gian chờ còn nguyên. |
| AC-25 | Hai quản lý cùng đọc version N | Cả hai cùng phân công hoặc đổi status | Chỉ một command commit; command còn lại nhận `409` và trạng thái/version hiện tại; không có hai người phụ trách chính hoặc sự kiện mồ côi. |
| AC-26 | Tài khoản quản trị viên chỉ có role quản trị | Cố xem phiếu hoặc đóng phiếu; quản lý cố sửa membership | Cả hai thao tác bị từ chối theo ma trận quyền. |

## 4. Quy tắc xử lý lỗi và mức chứng cứ

- `422`: dữ liệu thiếu/sai; không tạo thay đổi nghiệp vụ. `409`: trạng thái không hợp lệ hoặc version cũ; trả thông tin cần thiết để tải lại. `403`: hành động không được cấp trên đối tượng đã được phép đọc. `404`: đối tượng không tồn tại hoặc nằm ngoài phạm vi đọc. Không để khác biệt response làm lộ đối tượng của người khác.
- Test thành công phải kiểm tra response, trạng thái DB và event; test thất bại phải kiểm tra **không có** thay đổi DB hoặc tác dụng phụ. Các ca AC-07, AC-13, AC-19 phải kiểm tra cả API tệp và danh sách, không chỉ trang chi tiết.
- Dữ liệu demo chỉ chứng minh CivicFix chạy đúng **theo quy tắc đã giả lập**. Chưa thể kết luận cư dân sẽ dùng sản phẩm, nhân viên có thời gian nhập hộ, hoặc quy trình này hợp với chung cư thật. Các điểm đó vẫn ở [sổ giả định](ASSUMPTIONS.md).
