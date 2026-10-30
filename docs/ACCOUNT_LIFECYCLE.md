# Vòng đời tài khoản cư dân

**Trạng thái:** Quyết định sản phẩm và hợp đồng triển khai cho bản portfolio. Tất cả danh tính, email và căn hộ trong demo là giả. Chưa được dùng quy trình này với cư dân thật khi chưa có đơn vị vận hành chịu trách nhiệm xác minh và xử lý dữ liệu.

## 1. Quyết định cốt lõi

- **Quản trị viên chung cư** tạo lời mời và xác nhận liên kết cư dân–căn hộ dựa trên bằng chứng ngoài CivicFix. Quản lý phiếu và kỹ thuật viên không có quyền này nếu không được cấp thêm role quản trị.
- **Cư dân không tự đăng ký và không tự nhập số căn hộ để nhận quyền.** Họ mở lời mời, xác nhận quyền kiểm soát email, tự đặt mật khẩu và đăng nhập.
- Email hợp lệ **chỉ chứng minh quyền kiểm soát hộp thư**, không chứng minh người nhận là cư dân căn hộ đó. Trách nhiệm xác minh căn hộ thuộc quản trị viên; hệ thống lưu người xác minh, thời điểm và căn cứ tối thiểu.
- Lời mời chưa hoàn tất không cấp quyền đọc phiếu. Tài khoản bị vô hiệu hóa, membership hết hiệu lực hoặc liên kết căn hộ bị thu hồi phải mất quyền tương ứng ngay tại backend.
- Quản trị viên không đặt, xem, gửi hay khôi phục mật khẩu của cư dân. Không gửi mật khẩu tạm qua email/chat.

## 2. Người dùng và điểm bắt đầu

| Người | Thao tác | Màn hình |
|---|---|---|
| Quản trị viên | Tìm người đã có, mời mới, xác minh căn hộ, gửi lại/thu hồi lời mời, kết thúc membership | Tài khoản cư dân, Mời cư dân, Chi tiết cư dân |
| Cư dân được mời | Mở liên kết, xem đúng chung cư/căn hộ, đặt mật khẩu, đăng nhập | Kích hoạt tài khoản, Kích hoạt thành công, Đăng nhập |
| Cư dân quên mật khẩu | Yêu cầu liên kết đặt lại, đặt mật khẩu mới | Quên mật khẩu, Đặt lại mật khẩu |
| Cư dân không còn quyền | Nhận thông báo chung, không thấy thông tin phiếu | Không thể truy cập |

Trong demo cục bộ, seed một quản trị viên và các cư dân **giả**. Dịch vụ email chỉ gửi vào hộp thư thử nghiệm cục bộ (ví dụ Mailpit); không gửi tới địa chỉ người thật. Cần kiểm thử luồng mời thật từ trình duyệt tới database, không thay bằng một nút “kích hoạt” giả trên giao diện.

## 3. Luồng mời và kích hoạt

1. Quản trị viên tìm theo email đã chuẩn hóa trong **đúng chung cư**. Nếu đã có tài khoản, hệ thống tái sử dụng danh tính đó, không tạo user trùng. Giao diện nội bộ không cần tiết lộ dữ liệu từ chung cư khác.
2. Quản trị viên nhập tên hiển thị, email, căn hộ đã xác minh và ngày hiệu lực. Họ chọn căn cứ xác minh từ danh mục tối thiểu, ví dụ “đối chiếu danh sách cư dân được đơn vị vận hành cung cấp”; không tải giấy tờ cá nhân lên bản demo.
3. Màn hình xác nhận nêu rõ **ai được mời**, **quyền cư dân ở chung cư nào**, **căn hộ nào** và email sẽ nhận lời mời. Quản trị viên gửi lời mời; hệ thống ghi actor, thời điểm, membership chờ kích hoạt và sự kiện audit.
4. Email chỉ chứa mục đích, tên chung cư giả trong demo, thời hạn và liên kết cùng miền CivicFix. Không gửi mật khẩu, thông tin phiếu hoặc dữ liệu cư dân khác.
5. Người nhận mở liên kết còn hạn. Trang kích hoạt hiển thị chung cư và căn hộ để họ phát hiện mời nhầm. Nếu thông tin sai, họ chọn **Báo thông tin không đúng**, hệ thống không kích hoạt và tạo việc để quản trị viên kiểm tra.
6. **Người chưa có tài khoản** tự đặt mật khẩu. Server xác thực token dùng một lần và việc đặt mật khẩu trong cùng giao dịch; đánh dấu email đã xác nhận, tài khoản/membership hoạt động, token đã dùng, ghi audit. Người nhận đăng nhập bằng mật khẩu mới.
7. **Người đã có tài khoản hoạt động** đăng nhập bằng mật khẩu hiện tại rồi chấp nhận lời mời trong phiên gắn với đúng email. Server không đổi mật khẩu và chỉ kích hoạt membership/liên kết căn hộ được mời. Nếu user đang `DISABLED`, lời mời không âm thầm bật lại user; quản trị viên phải xử lý trạng thái này riêng và có audit.
8. Liên kết hết hạn, đã dùng hoặc bị thu hồi đều dẫn đến trạng thái không kích hoạt. Giao diện hướng dẫn liên hệ quản trị viên qua kênh đã công bố, không tiết lộ nội dung token hay tài khoản của người khác.

**Thời hạn đề xuất cho demo:** 24 giờ. Gửi lại tạo token mới và vô hiệu hóa token cũ. Chỉ một lời mời đang hiệu lực cho cùng người/chung cư/role; gửi lặp hoặc double-click không tạo hai membership. Token ngẫu nhiên đủ mạnh, chỉ lưu hash ở server, dùng một lần, không ghi URL chứa token vào log/analytics.

**Nội dung email thử nghiệm:** Tiêu đề “Lời mời tham gia CivicFix – Chung cư An Bình”; thân thư nêu tên chung cư, căn hộ được mời, thời hạn 24 giờ, nút “Xem lời mời”, và câu “Nếu bạn không mong đợi lời mời này, không mở liên kết; liên hệ đầu mối quản trị đã công bố.” Với tài khoản đã có, thư nói rõ sẽ đăng nhập bằng mật khẩu hiện tại. Không đưa căn cứ xác minh, thông tin phiếu hay tên cư dân khác vào email.

## 4. Đăng nhập, khôi phục và đăng xuất

- Đăng nhập bằng email và mật khẩu. Lỗi “email không tồn tại”, “sai mật khẩu”, “chưa kích hoạt” và “đã bị khóa” dùng một thông báo ngoài giao diện đủ chung để không dò tài khoản. Có giới hạn tốc độ và ghi nhận hành vi bất thường.
- Server trả quyền từ membership **đang có hiệu lực**, không tin role hoặc căn hộ do client gửi. Sau đăng nhập và mỗi request nhạy cảm, backend kiểm tra lại quyền; frontend chỉ dùng dữ liệu trả về từ `/auth/me` để chọn giao diện.
- Cư dân chọn “Quên mật khẩu”, nhập email và luôn nhận cùng thông báo: “Nếu email khớp với tài khoản đủ điều kiện, chúng tôi sẽ gửi hướng dẫn.” Liên kết đặt lại là token riêng, ngẫu nhiên, chỉ dùng một lần và hết hạn. Đặt lại mật khẩu không tự kích hoạt membership đang chờ hoặc đã bị thu hồi.
- Sau khi đặt lại mật khẩu thành công, gửi thông báo tới email đã xác nhận và thu hồi các phiên đăng nhập cũ. Nếu cư dân không còn kiểm soát email, họ liên hệ quản trị viên để xác minh lại ngoài ứng dụng; quản trị viên không tự tiết lộ link reset qua chat không xác thực.
- Email đặt lại có tiêu đề “Đặt lại mật khẩu CivicFix”, chỉ chứa link một lần, thời hạn, và cách bỏ qua nếu người nhận không yêu cầu. Email sau khi đổi thành công chỉ xác nhận đã đổi và hướng dẫn báo quản trị viên nếu không phải họ thực hiện; không chứa mật khẩu.
- Đăng xuất thu hồi phiên hiện tại. Thu hồi quyền, đổi mật khẩu hoặc vô hiệu hóa tài khoản phải làm phiên cũ mất hiệu lực.

**Chính sách mật khẩu đề xuất:** nếu chỉ dùng mật khẩu làm yếu tố đăng nhập, tối thiểu 15 ký tự, cho phép cụm từ dài, dán từ trình quản lý mật khẩu và ít nhất 64 ký tự; kiểm tra mật khẩu phổ biến/lộ lọt, không ép ký tự đặc biệt hay thay định kỳ vô cớ. Lưu bằng thuật toán hash mật khẩu phù hợp, không bao giờ lưu plaintext. Chính sách này tham khảo [NIST SP 800-63B](https://pages.nist.gov/800-63-4/sp800-63b/passwords/); thông báo lỗi chung và token khôi phục theo [OWASP Authentication](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html) và [Forgot Password](https://cheatsheetseries.owasp.org/cheatsheets/Forgot_Password_Cheat_Sheet.html).

## 5. Căn hộ, chuyển đi và dữ liệu lịch sử

- Một cư dân có thể liên kết nhiều căn hộ đã xác minh; một căn hộ có thể có nhiều cư dân. Chỉ quản trị viên thêm, sửa ngày hiệu lực hoặc kết thúc liên kết, có audit và căn cứ.
- Khi chuyển căn hộ, kết thúc liên kết cũ rồi xác minh liên kết mới. Phiếu cũ giữ `apartment_id_at_report` và nhãn căn hộ tại thời điểm báo; không tự thay đổi lịch sử.
- Khi rời chung cư, quản trị viên kết thúc membership và mọi liên kết căn hộ còn hiệu lực. Mọi phiên cũ mất quyền ngay; cư dân không xem lại phiếu qua tài khoản đã rời. Phiếu và audit vẫn còn cho người có quyền xử lý tại chung cư.
- Nếu quản trị viên mời sai email/căn hộ, họ thu hồi lời mời trước khi kích hoạt. Nếu đã kích hoạt nhầm, phải vô hiệu hóa membership và phiên ngay, ghi lý do, rồi xử lý dữ liệu có thể đã lộ theo quy trình của đơn vị vận hành. Không chỉ sửa nhãn căn hộ cho “đẹp”.

## 6. Trạng thái và dữ liệu tối thiểu

| Đối tượng | Trạng thái | Ý nghĩa |
|---|---|---|
| Lời mời | `PENDING`, `ACCEPTED`, `EXPIRED`, `REVOKED` | Chỉ `PENDING` còn hạn mới có thể kích hoạt; trạng thái cuối không tái dùng token |
| User | `INVITED`, `ACTIVE`, `DISABLED` | `INVITED` chưa đăng nhập được; `DISABLED` mất mọi phiên |
| Membership | `PENDING`, `ACTIVE`, `ENDED` | Chỉ `ACTIVE` trong khoảng hiệu lực được cấp role |
| Liên kết căn hộ | `VERIFIED` còn hiệu lực hoặc đã kết thúc | Quyền chọn căn hộ lấy từ liên kết hiệu lực; lời tự khai không phải xác minh |

Các trường tối thiểu: user id, email chuẩn hóa, tên hiển thị, email verified-at, password hash, user status; membership gồm building/role/status/valid-from/valid-to; liên kết căn hộ gồm apartment, verified-by/at, verification-source và thời hạn; invitation gồm email đích, role/building, token hash, created/expires/used/revoked-at; audit gồm actor, action, target, time và lý do. Không để token, mật khẩu hoặc thông tin định danh thừa trong audit.

## 7. Biên giao diện và API cần triển khai

| Giao diện | Hành động backend | Điều kiện chính |
|---|---|---|
| Danh sách cư dân / mời | Tìm và tạo lời mời | Admin đúng chung cư, không có membership trùng đang hiệu lực |
| Gửi lại / thu hồi lời mời | Tạo token mới / vô hiệu hóa token | Admin đúng chung cư; trạng thái `PENDING` |
| Kích hoạt | Kiểm tra token, đặt mật khẩu | Token đúng, còn hạn, chưa dùng, lời mời chưa bị thu hồi |
| Đăng nhập / đăng xuất / `/auth/me` | Tạo, thu hồi, đọc phiên | User và membership còn hiệu lực; quyền trả từ server |
| Quên / đặt lại mật khẩu | Tạo và dùng reset token | Trả thông báo chung; token riêng, một lần, hết hạn |
| Chi tiết cư dân | Xem và kết thúc liên kết căn hộ/membership | Admin đúng chung cư; có audit; thu hồi quyền ngay |

Giao diện phải có trạng thái đang gửi, thành công, lỗi gần trường nhập, link hết hạn, email gửi lại, mạng lỗi và double-click. Nút “Gửi lại” không tự gửi nhiều email liên tục; phản hồi đủ chung ở các bề mặt công khai để tránh dò email.

## 8. Ngoài phạm vi bản portfolio

Không có đăng ký tự do, đăng nhập mạng xã hội, SMS OTP, tải giấy tờ cư dân, xác minh cư trú tự động, nhiều chung cư thật, hay gửi email ra ngoài môi trường demo. Nếu pilot thật, chủ sở hữu quy trình phải quyết định cách xác minh cư trú, đầu mối hỗ trợ, chính sách lưu/xóa dữ liệu và kênh liên hệ trước khi mở tài khoản thật.
