# PRD — CivicFix

**Phiên bản:** 1.0 — bản đề xuất để kiểm chứng

**Ngày:** 27/10/2026  
**Chủ sản phẩm:** Dự án cá nhân  
**Trạng thái:** Chưa được xác nhận với một ban quản lý chung cư cụ thể  
**Phạm vi thử nghiệm:** Một chung cư, sự cố kỹ thuật ở khu vực chung

> Tài liệu này chốt một thiết kế sản phẩm có thể triển khai và kiểm thử. Những quy trình phụ thuộc vào chung cư thực tế được ghi là **giả định cần xác minh**. Không công bố CivicFix là kênh tiếp nhận chính thức, trực 24/7 hoặc cam kết thời gian xử lý khi chưa có đơn vị vận hành nhận trách nhiệm.

## 1. Tổng quan sản phẩm

CivicFix là sổ công việc chung cho việc tiếp nhận và xử lý sự cố kỹ thuật tại khu vực chung của một chung cư. Cư dân có thể báo qua web; nhân viên quản lý cũng có thể nhập hộ báo cáo nhận qua điện thoại, nhóm chat hoặc trực tiếp. Mỗi sự cố có một hồ sơ chính, người phụ trách, mốc thời gian, nhật ký xử lý và kết quả có thể đối chiếu.

Giá trị cần chứng minh không phải là số phiếu được tạo, mà là: ít báo cáo bị bỏ sót hơn, ít lần cư dân phải hỏi lại, quản lý biết ai đang chịu trách nhiệm và thông tin xử lý đáng tin. Nếu ban quản lý không có người duy trì sổ công việc hoặc cư dân thấy việc báo qua CivicFix chậm hơn kênh hiện có, sản phẩm chưa giải quyết được bài toán.

### 1.1. CivicFix sẽ làm gì?

- Cư dân gửi báo cáo sự cố kỹ thuật ở khu vực chung qua web và theo dõi tiến độ xử lý.
- Ban quản lý ghi nhận cả báo cáo đến từ điện thoại, nhóm chat hoặc trực tiếp; tiếp nhận, yêu cầu bổ sung thông tin, gộp báo trùng và phân công công việc.
- Kỹ thuật viên xem việc được giao, cập nhật quá trình sửa chữa và đề xuất hoàn thành.
- Ban quản lý xác nhận kết quả; cư dân phản hồi nếu sự cố vẫn tồn tại. Mọi quyết định quan trọng được lưu trong lịch sử.
- Hệ thống kiểm soát quyền xem theo chung cư và cung cấp số liệu vận hành sau khi dữ liệu đủ tin cậy.

## 2. Bài toán và bối cảnh

### 2.1. Vấn đề của từng bên

- **Cư dân:** Không biết báo cáo đã đến đúng người chưa, phải hỏi lại nhiều lần, có thể thấy “đã xong” dù hiện trường chưa được sửa.
- **Ban quản lý:** Báo cáo rải rác qua nhiều kênh; dễ thiếu vị trí, bỏ sót, giao trùng hoặc không rõ ai nhận việc; khó giải thích quá trình xử lý cho cư dân.
- **Kỹ thuật viên:** Nhận việc thiếu thông tin, bị hỏi lại trạng thái, phải cập nhật nhiều nơi; ghi nhật ký dài trên điện thoại gây cản trở công việc.

### 2.2. Giả thuyết cần kiểm chứng

1. Một người trong ban quản lý sẵn sàng nhập các báo cáo từ kênh ngoài vào CivicFix trong ca làm việc.
2. Cư dân chấp nhận dùng web nếu báo cáo nhanh, thấy đã được tiếp nhận và có thể phản hồi kết quả.
3. Kỹ thuật viên chấp nhận cập nhật vài trường ngắn tại hiện trường nếu không phải nhập lại ở kênh khác.
4. Dữ liệu trạng thái và thời điểm đủ tin cậy để giúp quản lý điều phối, không chỉ làm đẹp dashboard.
5. Một chung cư cụ thể có đầu mối và quy trình rõ cho sự cố nguy hiểm, ngoài giờ, nhà thầu và vật tư.

Nếu giả thuyết 1 không đúng, CivicFix cần tích hợp với kênh đang dùng hoặc thay đổi phạm vi sản phẩm. Không tiếp tục thêm dashboard để bù cho dữ liệu đầu vào thiếu.

## 3. Mục tiêu, ranh giới và điều kiện thành công

### 3.1. Mục tiêu của bản sản phẩm đầu tiên

1. Mọi báo cáo hợp lệ trong phạm vi thử nghiệm được ghi nhận thành một phiếu hoặc gắn vào phiếu hiện có.
2. Mỗi phiếu đang xử lý có người chịu trách nhiệm và hành động tiếp theo có thể xác định.
3. Người báo nhận được xác nhận tiếp nhận và xem được tiến độ có ý nghĩa.
4. Quản lý và kỹ thuật viên có thể hoàn thành một chu trình xử lý mà không cần nhập lại cùng thông tin nhiều lần.
5. Lịch sử đủ rõ để trả lời ai đã làm gì, lúc nào, vì sao; quyền truy cập không lộ dữ liệu cư dân khác.

### 3.2. Phi mục tiêu của bản đầu

- Không thay thế tổng đài, lực lượng ứng cứu hoặc hệ thống cảnh báo khẩn cấp.
- Không bảo đảm ban quản lý thực sự sửa chữa đúng hạn; phần mềm chỉ ghi nhận và hỗ trợ điều phối.
- Không xử lý thiết bị trong căn hộ riêng, tranh chấp cư dân, phí dịch vụ, khách ra vào.
- Không tự động phân loại bằng AI, dự đoán hỏng hóc, điều phối nhà thầu phức tạp hay vận hành nhiều chung cư.
- Không dùng chỉ số SLA để đánh giá nhân viên nếu nguồn dữ liệu chưa được kiểm chứng.

### 3.3. Điều kiện để thử nghiệm với người thật

- Có ban quản lý hoặc đầu mối được phép tham gia, xác nhận kênh nhận việc và giờ vận hành.
- Có người chịu trách nhiệm nhập báo cáo từ các kênh cũ và cập nhật tiến độ.
- Đã cấu hình số liên hệ cho sự cố nguy hiểm; có quy định tình huống nào phải gọi ngay.
- Chính sách dữ liệu, quyền xem, thời gian lưu và cách phản hồi khiếu nại đã được thống nhất.
- Dữ liệu thật không xuất hiện trên demo công khai.

## 4. Người dùng và quyền lợi

| Vai trò | Công việc cần hoàn thành | Điều họ quan tâm nhất |
|---|---|---|
| Cư dân | Báo hoặc theo dõi sự cố khu vực chung; bổ sung thông tin; phản hồi kết quả | Nhanh, rõ ràng, không phải hỏi lại |
| Nhân viên tiếp nhận/quản lý | Ghi nhận mọi nguồn, lọc trùng, yêu cầu bổ sung, đặt ưu tiên, giao việc, theo dõi tồn đọng | Không bỏ sót, có người chịu trách nhiệm |
| Kỹ thuật viên | Xem việc được giao, cập nhật ngắn gọn, báo chờ vật tư/nhà thầu, đính kèm kết quả | Thông tin đúng, thao tác ít trên điện thoại |
| Quản trị viên chung cư | Quản lý tài khoản, thành viên, đầu mối khẩn cấp, danh mục và quy tắc vận hành | Quyền rõ, không lộ dữ liệu |

Một người có thể có nhiều vai trò trong cùng chung cư, nhưng quyền được cấp rõ trong `building_memberships`. Không suy ra vai trò quản lý từ việc người đó sở hữu căn hộ. Trong bản đầu chỉ cấu hình một chung cư; mô hình quyền vẫn ghi rõ quan hệ với chung cư để tránh phải sửa logic bảo mật khi mở rộng.

## 5. Phạm vi phát hành theo mốc

### Mốc A — Kiểm chứng quy trình, chưa cần xây toàn bộ hệ thống

- Phỏng vấn cư dân, quản lý và kỹ thuật viên về **lần sự cố gần nhất**, cách báo, cách giao việc, nơi thông tin bị thất lạc.
- Quan sát hoặc mô phỏng việc tiếp nhận từ web, điện thoại/chat và xử lý báo trùng.
- Vẽ quy trình hiện tại; xác định ai sở hữu từng bước, ca ngoài giờ và kênh khẩn cấp.
- Chốt từ vựng mà người dùng hiểu: “đã nhận”, “đang xử lý”, “chờ”, “đã sửa”, “đã đóng”.
- **Điều kiện qua mốc:** Có bằng chứng về ít nhất một điểm nghẽn lặp lại và một đầu mối quản lý chấp nhận thử quy trình mới. Nếu chỉ làm portfolio, ghi rõ các phát hiện là giả lập hay phỏng vấn thật.

### Mốc B — Luồng cốt lõi có thể sử dụng

- Tài khoản được mời/khởi tạo bởi quản trị viên; phân quyền theo chung cư.
- Cư dân tạo phiếu nhanh; quản lý nhập hộ từ kênh ngoài.
- Danh sách, chi tiết, phân công, cập nhật tiến độ, lịch sử, ảnh tùy chọn.
- Yêu cầu bổ sung thông tin, gợi ý/gộp báo trùng, phản hồi chưa khắc phục.
- Hướng dẫn sự cố nguy hiểm và thông tin liên hệ trực tiếp.
- Thông báo trong ứng dụng và chỉ báo rõ rằng người dùng phải mở ứng dụng để đọc.
- **Điều kiện qua mốc:** Luồng đầu cuối chạy được với dữ liệu giả; các trường hợp quyền, trùng, thiếu thông tin, chuyển trạng thái cạnh tranh và lỗi upload được kiểm thử.

### Mốc C — Pilot và đo lường

- Cấu hình mốc phản hồi/khắc phục theo loại và ưu tiên sau khi ban quản lý đồng ý.
- Báo cáo cơ bản về đầu vào theo kênh, thời gian tiếp nhận, tồn đọng, yêu cầu bổ sung, mở lại và hỏi lại.
- Cơ chế xuất dữ liệu và xử lý sai sót cho người vận hành.
- Kiểm tra trải nghiệm trên điện thoại tại hiện trường; cải tiến theo dữ liệu thử nghiệm.
- **Điều kiện qua mốc:** Dữ liệu đủ đầy để giải thích từng chỉ số; người vận hành xác nhận dashboard phản ánh công việc thực.

### Mốc D — Mở rộng có điều kiện

Chỉ cân nhắc email/SMS/push, tích hợp kênh chat, nhà thầu, quản lý vật tư có cấu trúc hoặc nhiều chung cư khi pilot chứng minh nhu cầu và có người vận hành lâu dài. Đây không phải danh sách công việc đã cam kết.

## 6. Hành trình và quy tắc nghiệp vụ

### 6.1. Cư dân báo cáo qua web

1. Cư dân chọn “Báo sự cố khu vực chung”. Trước form, hệ thống hiển thị hướng dẫn gọi trực tiếp nếu có nguy hiểm tức thời (ví dụ người mắc kẹt trong thang máy, rò điện, ngập nước gần thiết bị điện). Số liên hệ lấy từ cấu hình đã được ban quản lý xác nhận.
2. Form ưu tiên vị trí và mô tả ngắn. Ảnh, danh mục và mức khẩn cấp gợi ý là tùy chọn hoặc có giá trị mặc định; không bắt cư dân tự chẩn đoán kỹ thuật.
3. Khi nhập vị trí/từ khóa, hệ thống có thể gợi ý các sự cố đang mở cùng khu vực. Cư dân được chọn “Tôi cũng gặp vấn đề này” để theo dõi; vẫn có thể gửi phiếu mới nếu không giống.
4. Sau khi gửi, màn hình hiển thị mã phiếu, thời điểm nhận, trạng thái ban đầu và kỳ vọng rõ: chưa đồng nghĩa đã có người đọc hoặc đang sửa.
5. Cư dân có thể bổ sung thông tin khi quản lý yêu cầu, xem tiến độ và phản hồi “vẫn còn lỗi” khi nhận kết quả.

**Tiêu chí chấp nhận R-01**

- Trên màn hình điện thoại, người dùng hoàn thành báo cáo hợp lệ chỉ với vị trí, mô tả và thao tác gửi; ảnh có thể bổ sung sau.
- Mỗi lần bấm gửi có khóa chống tạo phiếu trùng do gửi lặp hoặc mạng chập chờn.
- Sau khi gửi thành công, phiếu và sự kiện đầu tiên được ghi cùng transaction.
- Giao diện phân biệt “đã ghi nhận” với “đã được nhân viên tiếp nhận”.
- Người dùng không thể sửa vị trí, mô tả hoặc ảnh của người khác.

### 6.2. Quản lý nhập báo cáo từ kênh ngoài

Quản lý tạo phiếu với `source = PHONE | CHAT | IN_PERSON | OTHER`, thời điểm thực sự nhận báo cáo, tóm tắt, vị trí và thông tin liên hệ tối thiểu nếu cần. Có thể liên kết tài khoản cư dân nếu xác minh được; nếu không, phiếu vẫn xử lý được mà không tạo tài khoản giả. Không nhập nguyên văn đoạn chat chứa dữ liệu không cần thiết.

**Tiêu chí chấp nhận R-02**

- Phiếu nhập hộ hiển thị rõ người nhập và nguồn báo cáo; không giả mạo người báo.
- `received_at` là mốc nhận từ kênh ngoài; `created_at` là mốc nhập hệ thống. Cả hai không bị ghi đè âm thầm.
- Nếu có người báo đã liên kết tài khoản, người đó thấy phiếu sau khi quyền được xác minh.
- Nếu chưa liên kết, hệ thống không tự gửi thông tin cho một địa chỉ chưa xác thực.

### 6.3. Tiếp nhận, thiếu thông tin và từ chối

Quản lý xem hàng đợi mới, kiểm tra phạm vi khu vực chung, vị trí, mô tả, báo trùng và mức độ nguy hiểm. Có bốn hành động: tiếp nhận, yêu cầu thêm thông tin, đánh dấu trùng, hoặc từ chối có lý do. Yêu cầu bổ sung phải nêu câu hỏi cụ thể. Nếu người báo không phản hồi trong khoảng do chung cư cấu hình, quản lý có thể từ chối vì thiếu thông tin, nhưng phải lưu lý do; hệ thống không tự từ chối trong bản đầu.

**Tiêu chí chấp nhận R-03**

- Không thể từ chối hoặc đánh dấu trùng mà thiếu lý do/hồ sơ chính.
- Mọi quyết định có người thực hiện, thời điểm và ghi chú trong lịch sử.
- Người báo xem được lý do dễ hiểu, nhưng không thấy ghi chú nội bộ hoặc dữ liệu cư dân khác.
- Nếu sự cố bị từ chối sai, quản lý có thể khôi phục qua hành động có lịch sử; không xóa bản ghi cũ.

### 6.4. Báo trùng và người theo dõi

Hệ thống gợi ý theo vị trí, danh mục và các phiếu chưa đóng; đây chỉ là gợi ý. Quản lý quyết định hai báo cáo có cùng một sự cố vật lý hay không. Khi gộp, một phiếu là **hồ sơ chính**; phiếu còn lại được đóng với kết quả `DUPLICATE` và liên kết tới hồ sơ chính. Người báo phiếu phụ trở thành người theo dõi tiến độ công khai của hồ sơ chính. Không gộp hai sự cố chỉ vì cùng danh mục/tầng nếu chúng cần hai công việc sửa chữa khác nhau.

**Tiêu chí chấp nhận R-04**

- Chỉ quản lý được gộp hoặc tách gộp; thao tác và lý do được ghi lịch sử.
- Không tạo vòng liên kết và không gộp phiếu vào một phiếu phụ.
- Người theo dõi chỉ thấy tiêu đề/vị trí đã làm mờ nếu cần, trạng thái công khai và kết quả; không thấy danh tính, số căn hộ, ảnh riêng hay ghi chú nội bộ của người khác.
- Thông báo tiến độ không gửi trùng nhiều lần cho cùng một người.
- Tách gộp khôi phục khả năng xử lý độc lập và giữ nguyên dấu vết quyết định trước đó.

### 6.5. Phân công và thực hiện

Quản lý giao cho một kỹ thuật viên chịu trách nhiệm chính. Nếu cần nhà thầu, quản lý vẫn là người phụ trách điều phối trong CivicFix; bản đầu chưa cấp tài khoản nhà thầu. Kỹ thuật viên xem chi tiết cần thiết trên điện thoại, bấm bắt đầu, cập nhật ngắn gọn: đã kiểm tra gì, kết quả, hành động tiếp theo, ảnh tùy chọn. Vật tư và số phút thực hiện là trường tùy chọn trong bản đầu, vì ép nhập có thể tạo dữ liệu đối phó.

Khi thiếu vật tư, chờ nhà thầu hoặc không thể tiếp cận, kỹ thuật viên đề xuất tạm hoãn với lý do và mốc dự kiến cập nhật; quản lý xác nhận. Chuyển việc hoặc thu hồi phân công phải lưu lịch sử. Một phiếu chỉ có một người phụ trách chính tại một thời điểm.

**Tiêu chí chấp nhận R-05**

- Không giao cho tài khoản không hoạt động hoặc không có quyền kỹ thuật tại chung cư.
- Hai quản lý thao tác đồng thời không tạo hai phân công chính hoặc làm mất cập nhật của nhau.
- Kỹ thuật viên chỉ sửa phiếu đang được giao; sau khi bị thu hồi, quyền cập nhật dừng ngay.
- Ghi chú ngắn có thể được lưu mà không cần nhập vật tư hoặc số phút.
- Mỗi lần tạm hoãn có lý do, người xác nhận, thời điểm bắt đầu và thời điểm kết thúc.

### 6.6. Hoàn thành, xác nhận và mở lại

Kỹ thuật viên đánh dấu **đề xuất hoàn thành** bằng mô tả việc đã làm và kết quả kiểm tra. Quản lý xem bằng chứng và chuyển sang `RESOLVED`. Người báo nhận thông tin kết quả. Trong cửa sổ phản hồi cấu hình, người báo có thể chọn “Vẫn còn lỗi” và mô tả thêm; quản lý xác minh, sau đó mở lại cùng phiếu nếu cùng nguyên nhân/chưa sửa triệt để. Nếu là hỏng hóc mới sau khi đã ổn định, tạo phiếu mới và liên kết “tái diễn”.

Quản lý đóng phiếu khi đã kiểm tra kết quả. Nếu muốn đóng tự động sau cửa sổ phản hồi, phải được ban quản lý chấp thuận và hiển thị trước cho cư dân; bản đầu **không tự đóng**. Sau khi `CLOSED`, người báo vẫn có thể gửi phản hồi; quản lý quyết định mở lại hoặc tạo phiếu liên kết. “Mở lại” là một **sự kiện** đưa phiếu về `ASSIGNED` hoặc `IN_PROGRESS`, tăng `reopen_count`, không phải trạng thái đứng yên tên `REOPENED`.

**Tiêu chí chấp nhận R-06**

- Không chuyển `RESOLVED` nếu thiếu mô tả kết quả.
- Người báo thấy kết quả bằng ngôn ngữ dễ hiểu, không chỉ thấy mã trạng thái.
- Phản hồi chưa khắc phục không tự động tuyên bố lỗi của kỹ thuật viên; quản lý phải xác minh và ghi quyết định.
- Lịch sử giữ mọi lần giải quyết, đóng và mở lại; không ghi đè `resolved_at` cũ mà mất dấu vết.

## 7. Trạng thái và chuyển đổi

`status` là trạng thái hiện tại; `status_events` lưu mọi chuyển đổi. Lý do tạm hoãn, kết quả trùng, mức ưu tiên và người phụ trách là dữ liệu riêng, không được nhồi vào tên trạng thái.

| Trạng thái | Ý nghĩa hiển thị cho cư dân | Người/hành động đưa vào | Chuyển tiếp hợp lệ |
|---|---|---|---|
| `SUBMITTED` | Đã ghi nhận, chờ xem xét | Cư dân hoặc quản lý tạo | `NEEDS_INFO`, `ACCEPTED`, `REJECTED`, `CLOSED` với kết quả trùng |
| `NEEDS_INFO` | Cần thêm thông tin | Quản lý đặt câu hỏi | `SUBMITTED` khi bổ sung, `REJECTED` có lý do |
| `ACCEPTED` | Đã tiếp nhận xử lý | Quản lý | `ASSIGNED`, `ON_HOLD`, `REJECTED` nếu phát hiện ngoài phạm vi và có giải thích |
| `ASSIGNED` | Đã giao người xử lý | Quản lý phân công | `IN_PROGRESS`, `ON_HOLD`, `ACCEPTED` khi thu hồi |
| `IN_PROGRESS` | Đang xử lý | Kỹ thuật viên bắt đầu hoặc quản lý mở lại | `ON_HOLD`, `RESOLVED`, `ASSIGNED` khi đổi người |
| `ON_HOLD` | Đang chờ, có lý do và ngày cập nhật tiếp | Quản lý xác nhận | Trở về trạng thái trước tạm hoãn, hoặc `ASSIGNED` nếu đổi người |
| `RESOLVED` | Đã báo khắc phục, chờ xác nhận | Quản lý sau đề xuất của kỹ thuật viên | `CLOSED`, `ASSIGNED`, `IN_PROGRESS` khi mở lại |
| `CLOSED` | Đã đóng; vẫn có thể gửi phản hồi | Quản lý | `ASSIGNED`/`IN_PROGRESS` sau quyết định mở lại |
| `REJECTED` | Không tiếp nhận; có lý do | Quản lý | `SUBMITTED` khi khôi phục có lý do |

`SUBMITTED → CLOSED` chỉ hợp lệ với kết quả `DUPLICATE` và liên kết đến hồ sơ chính. Mọi ngoại lệ khác cần được định nghĩa trước khi đưa vào sản phẩm, không cho phép API “đặt status tùy ý”. Hành động khẩn cấp là một cờ và quy trình liên hệ, không là một trạng thái.

### 7.1. Quy tắc nhất quán

- Cập nhật trạng thái hiện tại, sự kiện lịch sử, phân công và thông báo trong một transaction database khi có thể.
- Sử dụng version/optimistic locking hoặc khóa hàng cho các hành động cạnh tranh; trả `409` kèm trạng thái mới nhất nếu xung đột.
- Mỗi command có idempotency key để chống bấm lặp khi mạng yếu.
- Không sửa/xóa lịch sử qua API thông thường. Sửa dữ liệu sai bằng sự kiện đính chính có người thực hiện và lý do.
- Mọi sự kiện chứa actor, nguồn, thời điểm server ghi nhận và dữ liệu liên quan; mốc do nhân viên nhập hồi tố được lưu riêng.

## 8. Ưu tiên, khẩn cấp và các mốc thời gian

### 8.1. Ưu tiên

Cư dân chọn mức lo ngại đơn giản: “Bình thường” hoặc “Có thể nguy hiểm/cần xử lý sớm”. Quản lý đặt `LOW`, `MEDIUM`, `HIGH`, `URGENT` sau khi xem xét. Mỗi thay đổi ưu tiên lưu giá trị trước/sau, người đổi, thời điểm, lý do. Cờ nguy hiểm của cư dân luôn hiển thị nổi bật cho người tiếp nhận dù ưu tiên chính thức khác đi.

### 8.2. Hướng dẫn ca nguy hiểm

Giao diện hiển thị: **Nếu có nguy hiểm tức thời, hãy gọi số trực ban/đơn vị ứng cứu được chung cư cung cấp. Gửi báo cáo trong ứng dụng không thay thế cuộc gọi.** Trước pilot phải xác nhận số, giờ hoạt động và trường hợp cần gọi. Không tự hiển thị một số mẫu trên môi trường thật. Nhân viên tiếp nhận có thể đánh dấu đã liên hệ qua kênh nào và lúc nào; không coi việc tạo phiếu là bằng chứng đã ứng cứu.

### 8.3. Ba đồng hồ khác nhau

| Chỉ số | Bắt đầu | Kết thúc | Ghi chú |
|---|---|---|---|
| Thời gian đến khi ghi nhận | `received_at` | `created_at` | Chỉ có ý nghĩa cho kênh ngoài; đo độ trễ nhập hộ. |
| Thời gian đến khi tiếp nhận | `received_at` | `accepted_at` hoặc quyết định từ chối | Đo tốc độ ban quản lý xem và quyết định. |
| Thời gian đến khi khắc phục | `received_at` | sự kiện `RESOLVED` đầu tiên/hiện hành | Báo cáo cả tổng thời gian và thời gian đang chờ; không xóa thời gian chờ khỏi số tổng. |

Các ngưỡng mục tiêu theo ưu tiên, giờ làm việc và loại sự cố là **cấu hình cần chốt với đơn vị vận hành**; tài liệu không tự đặt số giờ giả. Khi ưu tiên đổi, giữ hạn cũ và hạn mới để giải thích báo cáo. Khi `ON_HOLD`, đồng hồ tổng vẫn chạy; báo cáo có thể tách thời gian chờ theo lý do nhưng không âm thầm loại bỏ. Khi mở lại, báo cáo nêu lần khắc phục trước và thời gian đến lần khắc phục cuối cùng. Sự cố trùng và bị từ chối không được tính như phiếu sửa chữa hoàn tất.

## 9. Thông báo và kỳ vọng phản hồi

- Thông báo trong ứng dụng gửi cho người báo/người theo dõi khi: yêu cầu bổ sung, tiếp nhận, đổi trạng thái có ý nghĩa, kết quả, từ chối, gộp/tách gộp, mở lại.
- Người phụ trách nhận thông báo khi được giao, bị thu hồi, có phản hồi chưa khắc phục hoặc có việc cần cập nhật.
- Ban quản lý thấy hàng đợi chưa đọc, phiếu cần quyết định và phiếu quá mốc mục tiêu trên màn hình làm việc.
- Nội dung cho cư dân dùng ngôn ngữ bình thường, ví dụ “Đã giao kỹ thuật kiểm tra” thay vì chỉ `ASSIGNED`.
- Trong bản đầu, thông báo chỉ xuất hiện khi người dùng mở ứng dụng. Giao diện và hướng dẫn sử dụng phải nói rõ điều này; không hứa cảnh báo tức thời.
- Thông báo được tạo sau khi thay đổi nghiệp vụ commit thành công. Nếu gửi qua kênh ngoài ở giai đoạn sau, dùng hàng đợi/outbox để tránh gửi trước khi transaction hoàn tất.

## 10. Quyền truy cập và dữ liệu riêng tư

### 10.1. Ma trận quyền chính

| Hành động | Cư dân | Quản lý | Kỹ thuật viên | Quản trị viên |
|---|---|---|---|---|
| Tạo phiếu từ web | Có | Có, kể cả nhập hộ | Theo quyền được cấp | Có thể hỗ trợ |
| Xem phiếu đầy đủ | Phiếu của mình | Phiếu thuộc chung cư được quản lý | Phiếu đang được giao | Theo quyền quản trị rõ ràng |
| Xem tiến độ hồ sơ chính do báo trùng | Bản công khai giới hạn | Đầy đủ | Nếu được giao | Theo quyền |
| Gộp/tách gộp, ưu tiên, phân công | Không | Có | Không | Chỉ khi được cấp quyền quản lý |
| Ghi công việc | Không | Ghi chú điều phối | Phiếu đang được giao | Không mặc định |
| Xác nhận giải quyết/đóng | Phản hồi kết quả | Có | Đề xuất hoàn thành | Không mặc định |
| Quản lý tài khoản/cấu hình | Không | Không mặc định | Không | Có |

Quyền được kiểm tra tại từng endpoint và từng đối tượng. API trả cùng một kiểu lỗi phù hợp khi không tồn tại hoặc không có quyền để tránh lộ mã phiếu. Tệp đính kèm dùng storage riêng tư và URL có thời hạn; ảnh của phiếu chính không tự động mở cho mọi người theo dõi.

### 10.2. Danh tính và phạm vi chung cư

- `users` chứa danh tính tài khoản; `building_memberships(user_id, building_id, role, active, valid_from, valid_to)` xác định quyền tại chung cư.
- Cư dân có liên kết căn hộ đã xác minh riêng; một căn hộ có thể có nhiều cư dân và một cư dân có thể chuyển đi. Không dùng `users.apartment_id` làm nguồn duy nhất cho quyền truy cập.
- Tài khoản được mời hoặc tạo bởi quản trị viên. Không tự đăng ký rồi tự chọn căn hộ/role. Khi rời chung cư hoặc nghỉ việc, membership bị vô hiệu hóa; lịch sử vẫn giữ actor cũ.
- Nhân viên chỉ thấy phiếu thuộc chung cư mà membership còn hiệu lực. Bản đầu có một chung cư nhưng vẫn kiểm tra quan hệ này ở backend.
- Không đưa tên, số căn hộ, email hoặc số điện thoại người báo vào thông báo công khai của sự cố trùng.

### 10.3. Lưu giữ và xóa dữ liệu

Trước pilot cần chốt thời gian lưu ảnh, nội dung báo cáo, thông tin liên hệ và nhật ký truy cập với đơn vị vận hành. Bản demo dùng dữ liệu giả. Người vận hành có thể xuất dữ liệu theo quyền; yêu cầu xóa/đính chính thông tin cá nhân cần quy trình riêng để không phá lịch sử xử lý. Không tuyên bố tuân thủ một bộ quy định cụ thể khi chưa được rà soát.

## 11. Yêu cầu giao diện và trải nghiệm

### 11.1. Cư dân

- Thiết kế ưu tiên màn hình điện thoại; nút báo sự cố dễ tìm.
- Form ngắn, nhãn vị trí có ví dụ, ảnh tùy chọn; xem trước ảnh và xóa trước khi gửi.
- Sau khi gửi, hiển thị mã phiếu, mốc thời gian, bước tiếp theo và kênh khẩn cấp nếu cần.
- Trang chi tiết có dòng thời gian bằng ngôn ngữ dễ hiểu; phân biệt cập nhật công khai và ghi chú nội bộ.
- Nếu sự cố đã có người báo, thao tác “Theo dõi sự cố này” phải rõ hơn việc gửi phiếu trùng.
- Trạng thái chờ phải có lý do và mốc cập nhật tiếp theo; không để “đang xử lý” vô thời hạn mà không có thông tin.

### 11.2. Ban quản lý

- Hàng đợi mặc định ưu tiên phiếu chưa được xem, cờ nguy hiểm, phiếu thiếu chủ sở hữu và phiếu cần quyết định.
- Tạo phiếu nhập hộ nhanh từ cuộc gọi/chat; `source` và `received_at` nổi bật.
- Màn hình chi tiết cho thấy ai phụ trách, hành động kế tiếp, lần cập nhật cuối, các phiếu có thể trùng và lịch sử.
- Mỗi hành động thay đổi nghiệp vụ cho biết hệ quả trước khi xác nhận, đặc biệt là gộp, từ chối và đóng.

### 11.3. Kỹ thuật viên

- Danh sách “Việc của tôi” dùng tốt trên điện thoại; vị trí, mức ưu tiên, hướng dẫn tiếp cận và ảnh nằm ở đầu.
- Cập nhật ngắn theo hành động; không bắt viết cùng một thông tin vào chẩn đoán, công việc và ghi chú.
- Có thể lưu bản nháp cục bộ hoặc báo rõ dữ liệu chưa gửi khi kết nối yếu. Bản đầu chưa cam kết hỗ trợ hoàn toàn offline.
- Không hiển thị toàn bộ dữ liệu cá nhân của người báo nếu không cần để sửa chữa.

## 12. Mô hình dữ liệu ở mức sản phẩm

| Thực thể | Dữ liệu/quy tắc chính |
|---|---|
| `buildings` | Tên, địa chỉ, múi giờ, kênh liên hệ khẩn cấp và trạng thái pilot. |
| `users` | Danh tính, thông tin đăng nhập, trạng thái; không chứa role toàn cục làm nguồn quyền duy nhất. |
| `building_memberships` | User, building, role, hiệu lực; unique membership đang hoạt động phù hợp. |
| `resident_apartment_links` | Liên kết người–căn hộ, trạng thái xác minh và thời hạn. |
| `issues` | Mã, building, nguồn, người nhập, người báo nếu xác minh được, `received_at`, `created_at`, vị trí, mô tả, priority, status, version, người phụ trách hiện tại. |
| `issue_reporters` | Người báo/người theo dõi đã xác minh, quyền xem và nguồn tham gia. |
| `issue_links` | Quan hệ duplicate/recurrent/related, người quyết định và lý do. |
| `issue_events` | Nhật ký bất biến: loại sự kiện, actor, timestamp, dữ liệu trước/sau hoặc tham chiếu, ghi chú công khai/nội bộ. |
| `assignments` | Người được giao, người giao, thời điểm bắt đầu/kết thúc, lý do thu hồi/chuyển. |
| `work_updates` | Ghi chú công việc ngắn, kết quả, vật tư/phút nếu có, người tạo, thời điểm. |
| `attachments` | Storage key, loại, kích thước, người tải, quyền hiển thị; không lưu ảnh trong DB. |
| `notifications` | Người nhận, sự kiện nguồn, nội dung, `read_at`; chống tạo trùng theo sự kiện/người nhận. |
| `service_targets` | Cấu hình mục tiêu theo chung cư/ưu tiên/loại, hiệu lực theo thời gian; không sửa hồi tố âm thầm. |

Mọi timestamp lưu theo UTC và hiển thị theo múi giờ của chung cư. `received_at` nhập hồi tố phải có `recorded_by` và `recorded_at` để phân biệt với sự kiện server ghi nhận. Các FK và ràng buộc uniqueness bảo vệ toàn vẹn cơ bản; role và state machine được kiểm tra trong nghiệp vụ backend. Migration dùng Flyway.

## 13. API và hợp đồng hành vi

API REST dùng `/api/v1`; endpoint chính là command theo nghiệp vụ, không cho client PATCH tùy ý `status`, `assigned_to`, `building_id` hoặc `reported_by`.

| Nhóm | Endpoint dự kiến | Quy tắc chính |
|---|---|---|
| Phiếu | `POST /issues`, `GET /issues`, `GET /issues/{id}` | Danh sách được lọc theo quyền phía server; tạo phiếu có idempotency key. |
| Nhập hộ | `POST /issues/intake` | Chỉ quản lý; nguồn và thời điểm nhận riêng. |
| Bổ sung | `POST /issues/{id}/request-info`, `POST /issues/{id}/provide-info` | Chỉ đúng người/đúng trạng thái. |
| Tiếp nhận | `POST /issues/{id}/accept`, `/reject`, `/restore` | Ghi lý do và lịch sử. |
| Báo trùng | `GET /issues/suggestions`, `POST /issues/{id}/merge`, `/unmerge`, `/follow` | Trả bản công khai giới hạn cho cư dân. |
| Điều phối | `POST /issues/{id}/assign`, `/unassign`, `/priority`, `/hold`, `/resume` | Kiểm tra membership, trạng thái, version. |
| Công việc | `POST /issues/{id}/work-updates`, `/propose-resolution` | Chỉ kỹ thuật viên đang được giao. |
| Kết quả | `POST /issues/{id}/resolve`, `/close`, `/feedback`, `/reopen` | Phân biệt đề xuất, xác nhận, phản hồi và quyết định mở lại. |
| Tệp | `POST /issues/{id}/attachments`, `GET /attachments/{id}/download` | Kiểm tra quyền mỗi lần; URL ngắn hạn. |
| Thông báo | `GET /notifications`, `PATCH /notifications/{id}/read` | Chỉ chủ thông báo. |
| Cấu hình | `/admin/memberships`, `/admin/categories`, `/admin/emergency-contacts`, `/admin/service-targets` | Chỉ role được cấp; audit thay đổi. |

Response lỗi thống nhất với `code`, `message`, `details`, `requestId`. `409` trả trạng thái/version hiện tại khi có xung đột. `404` hoặc `403` theo chính sách chống lộ đối tượng. API docs phải chỉ rõ ai có quyền gọi, trạng thái trước/sau, tác dụng phụ và lỗi có thể gặp.

## 14. Yêu cầu phi chức năng và bảo mật

- **Khả dụng:** Giao diện web responsive và dùng được bằng bàn phím; nhãn form, lỗi và trạng thái đủ rõ cho công nghệ hỗ trợ. Kiểm tra với điện thoại phổ biến trong pilot.
- **Hiệu năng:** Các thao tác danh sách có phân trang, lọc phía server; định nghĩa ngưỡng mục tiêu sau khi có dữ liệu và môi trường pilot, tránh cam kết số tùy tiện.
- **Độ tin cậy:** Backup dữ liệu khi có người thật; thử khôi phục trước pilot. Có log lỗi, request ID và cảnh báo khi backend/storage lỗi kéo dài.
- **Xác thực:** Mật khẩu hash bằng thư viện chuẩn; session/token có thời hạn và cơ chế thu hồi phù hợp. Không lưu secret trong repo hay log.
- **Phân quyền:** Kiểm tra object-level cho phiếu, sự kiện, ảnh, thông báo; kiểm tra membership của chung cư ở backend. Test các ca đổi ID, tài khoản bị khóa, kỹ thuật viên bị thu hồi việc.
- **Upload:** Giới hạn số lượng/kích thước/loại ảnh, kiểm tra nội dung, khóa lưu do server tạo, storage riêng tư, dọn file mồ côi. Bản đầu chỉ nhận ảnh cần thiết.
- **Chống lạm dụng:** Giới hạn đăng nhập/tạo phiếu, chống gửi lặp, giới hạn độ dài văn bản; không dùng dữ liệu client gửi để xác định quyền.
- **Tính toàn vẹn:** Transaction cho status + event + assignment + notification; optimistic locking cho thao tác đồng thời; audit thay đổi quyền và cấu hình mục tiêu.
- **Triển khai:** Demo công khai chỉ dùng dữ liệu giả và tài khoản thử. Pilot thật cần môi trường và dữ liệu tách biệt.

## 15. Đo lường và đánh giá

### 15.1. Chỉ số có ích trong pilot

| Chỉ số | Cách tính/điều cần lưu ý |
|---|---|
| Tỷ lệ ghi nhận | Số báo cáo thuộc phạm vi có phiếu / tổng báo cáo quan sát được qua các kênh. Cần đối chiếu với sổ/kênh cũ. |
| Độ trễ nhập hộ | `created_at - received_at` theo kênh ngoài; loại bỏ mốc không xác thực và nêu tỷ lệ thiếu dữ liệu. |
| Thời gian tiếp nhận | Từ nhận báo cáo đến quyết định tiếp nhận/từ chối; báo median và phân bố, không chỉ trung bình. |
| Phiếu không người phụ trách | Số/tuổi phiếu đã tiếp nhận nhưng chưa giao hoặc không có hành động tiếp theo. |
| Tỷ lệ báo trùng | Số báo cáo được gắn vào hồ sơ chính; theo dõi cả gộp nhầm bị tách. |
| Tỷ lệ phải hỏi lại | Số phiếu `NEEDS_INFO` / số phiếu mới; xem lý do thiếu thông tin. |
| Tỷ lệ mở lại | Số phiếu được mở lại / số phiếu đã báo khắc phục; phân biệt lỗi cũ và lỗi tái diễn. |
| Người dùng phải hỏi ngoài ứng dụng | Ghi nhận bằng phỏng vấn/nhật ký pilot; không suy ra chỉ từ event trong app. |
| Mức đầy đủ của dữ liệu | Tỷ lệ phiếu có nguồn, vị trí, người phụ trách, quyết định và mốc thời gian hợp lệ. |

Không dùng “số phiếu được tạo” hay “tỷ lệ hoàn thành đúng hạn” làm bằng chứng duy nhất về giá trị. Cần đối chiếu số liệu với thực tế vận hành và ghi các ca hệ thống không nắm được.

### 15.2. Nghiên cứu người dùng

- Phỏng vấn cư dân và nhân viên bằng câu chuyện về lần sự cố cụ thể gần nhất, không chỉ hỏi họ có thích ý tưởng hay không.
- Cho mỗi vai trò làm nhiệm vụ trên prototype/bản chạy; quan sát điểm dừng, thao tác ngoài app và cách họ hiểu trạng thái.
- Với người quản lý, thử tình huống một cuộc gọi, hai báo cáo trùng, sự cố ngoài giờ và chờ nhà thầu.
- Với kỹ thuật viên, thử cập nhật trên điện thoại tại nơi có kết nối yếu và khi tay đang bận.
- Ghi lại phát hiện, quyết định thay đổi và điều chưa được xác minh. Mẫu nhỏ giúp phát hiện lỗi, không đủ để khẳng định toàn thị trường.

## 16. Kiểm thử và tiêu chí chấp nhận tổng thể

### 16.1. Kiểm thử bắt buộc trước demo hoàn chỉnh

1. Cư dân gửi phiếu; bấm gửi lặp không tạo hai phiếu; lịch sử ban đầu đúng.
2. Quản lý nhập hộ; `received_at` và `created_at` khác nhau được giữ đúng.
3. Hai báo cáo trùng được gộp rồi tách; người theo dõi không thấy dữ liệu riêng.
4. Thiếu thông tin → bổ sung → tiếp nhận; từ chối và khôi phục đều có lý do.
5. Phân công, đổi người, thu hồi; kỹ thuật viên cũ mất quyền cập nhật.
6. Tạm hoãn và tiếp tục giữ đúng trạng thái trước đó, lý do và mốc thời gian.
7. Đề xuất hoàn thành → quản lý xác nhận → cư dân báo vẫn lỗi → quản lý mở lại; lịch sử và mốc SLA không bị ghi đè.
8. Hai quản lý cập nhật cùng phiếu; một thao tác nhận xung đột rõ ràng, không mất dữ liệu.
9. Người ngoài phạm vi đổi ID phiếu/ảnh/thông báo không đọc được dữ liệu.
10. Upload lỗi giữa storage và DB không để lại ảnh công khai hoặc metadata sai; có dọn dẹp file mồ côi.

### 16.2. Định nghĩa hoàn thành cho mốc B

- Các luồng chính hoạt động đầu cuối trên web và điện thoại.
- Quyền và state machine được kiểm thử ở backend; không dựa vào nút ẩn trên frontend.
- Dữ liệu demo thể hiện ca bình thường, báo trùng, thiếu thông tin, chờ và mở lại.
- README có cách chạy, tài khoản demo, sơ đồ quyền, quyết định nghiệp vụ và giới hạn đã biết.
- Không có secret hay dữ liệu cá nhân thật trong repo/demo.
- Một người chưa biết sản phẩm có thể hoàn thành tác vụ của vai trò mình mà không cần người xây ngồi hướng dẫn.

## 17. Rủi ro và cách xử lý

| Rủi ro | Dấu hiệu sớm | Cách xử lý |
|---|---|---|
| Ban quản lý không nhập báo cáo từ kênh cũ | Phiếu trên app ít hơn thực tế | Rút gọn màn nhập hộ, phân người chịu trách nhiệm; nếu vẫn không được, đánh giá tích hợp kênh hiện dùng. |
| Cư dân tiếp tục hỏi qua chat | Hỏi lại nhiều dù đã có trang trạng thái | Cải thiện nội dung cập nhật, mốc phản hồi và khả năng truy cập; phỏng vấn nguyên nhân. |
| Kỹ thuật viên cập nhật đối phó | Nhiều ghi chú giống nhau/điền cuối ca | Bỏ trường không cần, tối ưu mobile, quan sát tại hiện trường. |
| Báo trùng bị gộp nhầm | Phiếu phụ phản hồi không liên quan | Gộp thủ công, có tách gộp và lịch sử; không tự động gộp bằng từ khóa. |
| Chỉ số SLA bị hiểu sai | Nhiều mốc nhập hồi tố/đổi ưu tiên/tạm hoãn | Hiển thị chất lượng dữ liệu và lịch sử thay đổi; không dùng báo cáo để đánh giá khi dữ liệu kém. |
| Người dùng tin form là kênh ứng cứu tức thời | Báo ca nguy hiểm rồi chờ thông báo trong app | Hướng dẫn gọi trực tiếp nổi bật; xác minh đầu mối trước pilot. |
| Lộ thông tin cư dân qua phiếu chung | Người theo dõi thấy ảnh, số căn hộ, danh tính | Bản công khai riêng; test quyền với mọi endpoint và file. |

## 18. Các quyết định cần xác minh với chung cư trước pilot

1. Ai trực tiếp nhận báo cáo, trong khung giờ nào? Sau giờ làm việc cư dân phải liên hệ đâu?
2. Những trường hợp nào phải gọi trực tiếp? Số điện thoại/đầu mối nào được phép hiển thị?
3. Ai nhập các báo cáo từ chat/điện thoại? Có quyền nhập thời điểm nhận hồi tố không và ai kiểm tra?
4. Ai xác nhận việc sửa xong? Cư dân có bao lâu để phản hồi trước khi quản lý đóng?
5. Khi chờ nhà thầu, vật tư hoặc không thể tiếp cận hiện trường, ai cập nhật và tần suất bao nhiêu?
6. Cư dân có được xem sự cố chung đang mở ở tầng/khu vực của mình không? Vị trí nào có thể gây lộ dữ liệu riêng?
7. Mục tiêu phản hồi/khắc phục theo loại và ưu tiên là gì? Có dùng giờ làm việc hay thời gian lịch?
8. Thời gian giữ ảnh và thông tin người báo là bao lâu? Ai xử lý yêu cầu đính chính/xóa?

Những câu trả lời này là đầu vào để chốt cấu hình và acceptance criteria của pilot. Với portfolio, có thể dùng **giả định mô phỏng được ghi rõ**, nhưng không trình bày chúng như chính sách đã được chung cư xác nhận.

## 19. Kết luận phạm vi

Bản đầu của CivicFix cần chứng minh một chu trình: **nhận báo cáo từ mọi kênh thực tế → lọc trùng/tiếp nhận → giao người chịu trách nhiệm → cập nhật công việc → xác nhận kết quả → cho người báo phản hồi**. Bảo mật, lịch sử và tính đúng của trạng thái là điều kiện nền. Dashboard và chỉ số SLA chỉ có giá trị sau khi dữ liệu đầu vào và quy trình cập nhật đã đáng tin.
