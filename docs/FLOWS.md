# Luồng nghiệp vụ CivicFix

**Phạm vi:** Bản web portfolio cho sự cố kỹ thuật ở khu vực chung của một chung cư. Các tình huống và thao tác liên hệ ngoài ứng dụng là mô phỏng. Sơ đồ này cụ thể hóa [PRD](PRD.md), không mô tả phần mềm đã triển khai.

## 0. Quản trị viên mời và kích hoạt tài khoản cư dân

```mermaid
flowchart TD
    A["Quản trị viên xác minh cư dân và căn hộ ngoài ứng dụng"] --> B["Tìm tài khoản hiện có; không tạo user trùng"]
    B --> C["Nhập email, căn hộ, ngày hiệu lực và căn cứ xác minh"]
    C --> D["Kiểm tra lại rồi gửi lời mời dùng một lần"]
    D --> E{"Người nhận mở link còn hạn?"}
    E -- Không --> F["Quản trị viên gửi lại hoặc thu hồi; link cũ mất hiệu lực"]
    E -- Có --> G{"Thông tin chung cư/căn hộ đúng?"}
    G -- Không --> H["Báo thông tin sai; chưa cấp quyền"]
    G -- Có --> I["Cư dân tự đặt mật khẩu"]
    I --> J["Server kích hoạt user và membership, ghi audit"]
    J --> K["Đăng nhập; backend lấy quyền và căn hộ còn hiệu lực"]
    K --> L{"Chuyển đi hoặc liên kết bị thu hồi?"}
    L -- Có --> M["Kết thúc membership/liên kết, thu hồi phiên; giữ lịch sử phiếu"]
```

Quản trị viên là người gắn căn hộ đã xác minh; xác nhận email chỉ chứng minh quyền kiểm soát hộp thư. Cư dân không tự chọn căn hộ hay role. Chi tiết trạng thái, khôi phục mật khẩu và xử lý lỗi nằm trong [vòng đời tài khoản](ACCOUNT_LIFECYCLE.md).

## 1. Bản đồ trách nhiệm từ báo cáo đến đóng phiếu

```mermaid
sequenceDiagram
    autonumber
    actor R as Cư dân
    participant S as CivicFix
    actor M as Ban quản lý
    actor T as Kỹ thuật viên
    R->>S: Gửi báo cáo khu vực chung
    S-->>R: Mã phiếu, SUBMITTED, thời điểm ghi nhận
    S-->>M: Phiếu trong hàng đợi tiếp nhận
    M->>S: Kiểm tra phạm vi, thông tin, nguy hiểm và báo trùng
    alt Cần bổ sung
        M->>S: Đặt câu hỏi cụ thể, NEEDS_INFO
        S-->>R: Thông báo trong ứng dụng
        R->>S: Bổ sung thông tin, về SUBMITTED
    else Ngoài phạm vi hoặc không đủ căn cứ
        M->>S: Từ chối kèm lý do, REJECTED
        S-->>R: Lý do có thể xem
    else Trùng cùng một sự cố vật lý
        M->>S: Liên kết hồ sơ chính, đóng phiếu phụ
        S-->>R: Xem tiến độ công khai của hồ sơ chính
    else Tiếp nhận
        M->>S: ACCEPTED, đặt ưu tiên và phân công
        S-->>T: Việc được giao, ASSIGNED
        T->>S: Bắt đầu, cập nhật và đề xuất hoàn thành
        M->>S: Kiểm tra kết quả, xác nhận RESOLVED
        S-->>R: Tạo thông báo kết quả trong ứng dụng
        alt Người báo phản hồi vẫn còn lỗi
            R->>S: Gửi phản hồi
            M->>S: Xác minh và mở lại hoặc tạo phiếu liên kết
        else Quản lý xác nhận đủ điều kiện đóng
            M->>S: CLOSED
        end
    end
```

Thông báo trong ứng dụng chỉ xuất hiện khi người dùng mở web. Tạo thông báo không chứng minh người đó đã đọc. Ban quản lý là người xác nhận kết quả và quyết định mở lại; kỹ thuật viên chỉ **đề xuất hoàn thành**.

## 2. Cư dân tự báo qua web

```mermaid
flowchart TD
    A["Phát hiện hư hỏng"] --> B{"Nguy hiểm tức thời?"}
    B -- Có --> C["Gọi đầu mối trực ban hoặc ứng cứu đã xác nhận"]
    B -- Không --> D{"Thiết bị thuộc khu vực chung?"}
    C --> D
    D -- Không --> E["Hiển thị kênh xử lý phù hợp; không tạo phiếu khu vực chung"]
    D -- Có --> F["Đăng nhập; nhập vị trí hỏng và mô tả"]
    F --> G["Hệ thống lấy người báo và căn hộ đã xác minh; vị trí hỏng lưu riêng"]
    G --> H{"Gợi ý phiếu đang mở có đúng cùng sự cố?"}
    H -- Có --> I["Chọn theo dõi hồ sơ chính"]
    H -- Không hoặc chưa chắc --> J["Gửi báo cáo mới"]
    J --> K["Backend kiểm tra quyền, dữ liệu và khóa chống gửi lặp"]
    K --> L["Lưu phiếu SUBMITTED và sự kiện đầu tiên cùng transaction"]
    L --> M["Hiện mã phiếu và trạng thái chờ xem xét"]
    I --> N["Xem tiến độ công khai, không thấy dữ liệu riêng của người khác"]
    M --> O["Chờ ban quản lý tiếp nhận và quyết định"]
```

- **Căn hộ người báo** dùng để đối chiếu danh tính. **Vị trí hỏng** là nơi cần sửa. Ví dụ cư dân căn 03 báo đèn hỏng ở hành lang trước căn 03; đây vẫn là sự cố khu vực chung. Ổ điện hoặc vòi nước riêng bên trong căn 03 nằm ngoài phạm vi bản đầu.
- Nếu tài khoản chưa có căn hộ được xác minh, vẫn nhận báo cáo và ghi “căn hộ chưa xác minh”. Không suy ra căn hộ từ mô tả vị trí. Nếu có nhiều căn hộ đã xác minh, cư dân chỉ chọn từ các căn hộ được liên kết với mình.
- Gợi ý trùng không tự gộp. Cư dân có thể tạo phiếu mới nếu đó là hư hỏng khác. Quyết định gộp/tách thuộc ban quản lý và phải có lý do.
- Cuộc gọi khẩn cấp là bước xử lý ngoài CivicFix; phiếu web không thay thế yêu cầu ứng cứu.

## 3. Ban quản lý phân loại và giao việc

```mermaid
flowchart TD
    A["Phiếu SUBMITTED trong hàng đợi"] --> B["Quản lý kiểm tra phạm vi, mức nguy hiểm, vị trí, mô tả và nguồn báo"]
    B --> C{"Đủ thông tin để quyết định?"}
    C -- Không --> D["Đặt câu hỏi cụ thể, NEEDS_INFO"]
    D --> E{"Người báo bổ sung?"}
    E -- Có --> A
    E -- Không, hết khoảng chờ cấu hình --> F["Quản lý có thể REJECTED kèm lý do"]
    C -- Có --> G{"Thuộc khu vực chung?"}
    G -- Không --> H["REJECTED kèm lý do và hướng dẫn phù hợp"]
    G -- Có --> I{"Cùng một sự cố vật lý với phiếu đang xử lý?"}
    I -- Có --> J["Quản lý gộp có lý do; phiếu phụ CLOSED với kết quả DUPLICATE"]
    I -- Không --> K["ACCEPTED; quản lý đặt ưu tiên"]
    K --> L["Giao một kỹ thuật viên đang có quyền tại chung cư"]
    L --> M["ASSIGNED; kỹ thuật viên nhận việc"]
```

Hệ thống không tự từ chối khi hết thời gian chờ và không tự gộp theo danh mục/tầng. Mỗi quyết định lưu người thực hiện, thời điểm và lý do. Việc gộp sai phải tách được, giữ lại lịch sử. Hai quản lý thao tác đồng thời cần được kiểm tra xung đột để không tạo hai người phụ trách chính.

## 4. Xử lý, báo kết quả và phản hồi

```mermaid
flowchart TD
    A["Kỹ thuật viên nhận phiếu ASSIGNED"] --> B["Bắt đầu IN_PROGRESS và ghi cập nhật"]
    B --> C{"Cần chờ vật tư, nhà thầu hoặc tiếp cận?"}
    C -- Có --> D["Đề xuất lý do và mốc cập nhật; quản lý xác nhận ON_HOLD"]
    D --> E["Quản lý cho tiếp tục về trạng thái trước khi chờ hoặc giao lại"]
    E --> B
    C -- Không --> F["Kỹ thuật viên mô tả việc đã làm, kết quả kiểm tra và đề xuất hoàn thành"]
    F --> G{"Quản lý chấp nhận kết quả?"}
    G -- Chưa --> B
    G -- Có --> H["Quản lý chuyển RESOLVED; hệ thống tạo thông báo hoặc việc liên hệ"]
    H --> I{"Người báo phản hồi vẫn còn lỗi?"}
    I -- Có --> J["Quản lý xác minh: cùng nguyên nhân hay hỏng hóc mới?"]
    J -- Cùng nguyên nhân --> K["Ghi sự kiện mở lại, tăng reopen_count; về ASSIGNED hoặc IN_PROGRESS"]
    K --> B
    J -- Hỏng hóc mới --> L["Tạo phiếu mới, liên kết tái diễn"]
    I -- Chưa hoặc không --> M{"Quản lý đã kiểm tra và đủ điều kiện đóng?"}
    M -- Chưa --> N["Giữ RESOLVED, tiếp tục theo dõi"]
    M -- Rồi --> O["Quản lý chuyển CLOSED, không tự đóng theo thời gian"]
    O --> P{"Có phản hồi sau khi đóng?"}
    P -- Có --> J
```

Nếu thu hồi hoặc chuyển việc, kỹ thuật viên cũ mất quyền cập nhật ngay. Mở lại là **sự kiện** chuyển về ASSIGNED/IN_PROGRESS, không phải một trạng thái REOPENED riêng. Với báo cáo ngoài web, điều kiện đóng còn phụ thuộc nhật ký liên hệ ở luồng 5.

## 5. Ban quản lý nhập hộ từ điện thoại, chat hoặc trực tiếp

```mermaid
flowchart TD
    A["Nhận báo cáo qua điện thoại, chat hoặc trực tiếp"] --> B{"Nguy hiểm tức thời?"}
    B -- Có --> C["Hướng dẫn kênh ứng cứu đã xác nhận; ghi nhận việc liên hệ nếu có"]
    B -- Không --> D{"Thuộc khu vực chung?"}
    C --> D
    D -- Không --> X["Hướng dẫn kênh phù hợp; không tạo phiếu khu vực chung"]
    D -- Có --> E["Quản lý nhập nguồn, received_at, mô tả, vị trí; hệ thống ghi created_at riêng"]
    E --> F{"Xác minh được tài khoản và căn hộ?"}
    F -- Có --> G["Liên kết người báo và căn hộ đã xác minh"]
    F -- Không --> H["Lưu thông tin liên hệ tối thiểu; căn hộ tự khai gắn nhãn chưa xác minh"]
    G --> I["Tạo SUBMITTED, hiển thị người nhập và nguồn"]
    H --> I
    I --> J["Dùng chung luồng phân loại, giao việc và xác nhận RESOLVED"]
    J --> K{"Người báo có tài khoản đã liên kết?"}
    K -- Có --> L["Tạo thông báo trong ứng dụng"]
    K -- Không --> M["Tạo việc quản lý cần liên hệ trả kết quả"]
    M --> N["Liên hệ qua kênh phù hợp; xác minh người nhận trước khi nói chi tiết"]
    N --> O{"Kết quả liên hệ?"}
    O -- Xác nhận đã nhận --> P["Ghi kênh, thời điểm, người thực hiện và xác nhận"]
    O -- Đã thử, chưa gặp --> Q["Ghi lần thử; không ghi là đã nhận"]
    O -- Không có kênh liên hệ --> R["Ghi lý do không thể liên hệ"]
    L --> S["Quản lý kiểm tra kết quả rồi quyết định đóng"]
    P --> S
    Q --> S
    R --> S
```

Phiếu nhập hộ không được đóng khi việc liên hệ còn trống: cần ít nhất một lần liên hệ đã ghi nhận hoặc lý do không thể liên hệ. “Đã thử” khác “người báo xác nhận đã nhận”. Nếu người báo nói sự cố vẫn còn qua điện thoại/chat, quản lý ghi phản hồi và xử lý theo luồng mở lại. Bản portfolio không tự gọi điện hoặc gửi tin nhắn.

## 6. Những ranh giới phải giữ khi triển khai

| Ranh giới | Quy tắc cần kiểm tra |
|---|---|
| Dữ liệu người báo | Ban quản lý thấy tên và căn hộ của phiếu thuộc chung cư mình. Cư dân chỉ thấy dữ liệu của mình. Kỹ thuật viên chỉ thấy thông tin thật sự cần cho việc được giao. |
| Phiếu báo trùng | Người theo dõi phiếu chính chỉ thấy tiến độ và kết quả công khai; không thấy tên, căn hộ, ảnh riêng hoặc ghi chú nội bộ của người khác. |
| Căn hộ và vị trí | Căn hộ được xác minh tại thời điểm báo được lưu thành snapshot; thay đổi nơi ở về sau không sửa lịch sử. Vị trí hỏng là trường độc lập. |
| Chuyển trạng thái | Backend kiểm tra vai trò, chung cư, quyền trên từng phiếu và trạng thái hiện tại. Không cho client tự gán status hoặc người báo. |
| Lịch sử | Mỗi quyết định, phân công, tạm hoãn, kết quả, liên hệ và mở lại có actor, thời điểm và sự kiện lưu vết. |
| Thông báo | Phân biệt đã tạo, đã đọc, đã thử liên hệ và đã xác nhận nhận kết quả. Không suy diễn một mốc thành mốc khác. |

**Ba kịch bản để kiểm tra sơ đồ:** (1) Cư dân căn 03 báo đèn hành lang và cư dân khác báo trùng; (2) quản lý nhập hộ cuộc gọi về rò nước đường ống chung rồi ghi lần trả kết quả; (3) kỹ thuật viên báo sửa xong nhưng cư dân phản hồi lỗi vẫn còn, quản lý mở lại cùng phiếu. Đây là dữ liệu giả lập; quy trình và ngưỡng thời gian cần xác minh với đơn vị vận hành trước khi dùng thật.
