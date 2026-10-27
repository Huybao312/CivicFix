# Roadmap CivicFix — hoàn thành từng luồng chức năng

**Cập nhật:** 27/10/2026

**Mục tiêu:** Web responsive portfolio cho quy trình xử lý sự cố kỹ thuật ở khu vực chung của một chung cư. Dữ liệu và tình huống là mô phỏng; chưa có xác nhận nhu cầu từ cư dân hoặc ban quản lý.

## 1. Cách đọc milestone

Mỗi milestone dưới đây nêu **một kết quả người dùng hoặc người vận hành có thể thực hiện được**. Hiện mỗi milestone có một issue theo dõi với checklist cụ thể. Khi bắt đầu làm, tách checkbox lớn thành issue nhỏ nếu cần; mỗi issue đi xuyên qua giao diện, API, database, phân quyền và kiểm thử. Không tạo các chặng riêng kiểu “xong frontend”, “xong backend”, “cuối cùng mới kiểm thử”.

Milestone không có ngày đến hạn cố định. Khoảng giờ là ước lượng để theo dõi sức làm, không phải cam kết. Bạn có thể điều chỉnh sau mỗi milestone, nhưng không được đánh dấu hoàn thành khi mới có màn hình giả hoặc endpoint chỉ chạy trong Postman.

### Điều kiện đóng một issue chức năng

Một issue được xem là xong khi các mục **có liên quan đến chức năng đó** đã hoàn tất; mục không áp dụng phải ghi lý do:

1. **Luồng chạy được:** người dùng thực hiện từ giao diện đến API và database; dữ liệu thật của ứng dụng quay lại giao diện.
2. **Quy tắc và lỗi:** backend kiểm tra dữ liệu, trạng thái, trường hợp thiếu/sai và request gửi lặp hoặc cạnh tranh nếu rủi ro này có mặt.
3. **Quyền và riêng tư:** kiểm tra vai trò, chung cư, quyền đối tượng và các trường được phép trả về; không dùng nút ẩn làm biện pháp bảo mật.
4. **Kiểm thử có giá trị:** test nghiệp vụ ở lớp phù hợp, ít nhất một đường thành công và một ca lỗi/quyền quan trọng. Chạy lại test hồi quy liên quan trước khi đóng.
5. **Trải nghiệm:** trạng thái tải, lỗi và rỗng dễ hiểu; luồng của cư dân/kỹ thuật viên dùng được trên điện thoại và bằng bàn phím.
6. **Tích hợp và tài liệu:** migration/seed nếu có thay đổi dữ liệu; API docs hoặc README được cập nhật; demo được chức năng từ dữ liệu giả mà không sửa DB thủ công.

Từ M2, cấu hình CI chạy build và các test hiện có cho mỗi lần cập nhật code. Security review là một phần của thiết kế từng issue, dùng [OWASP ASVS](https://asvs.owasp.org/) làm tài liệu tham khảo phù hợp với rủi ro; không tuyên bố chứng nhận bảo mật.

## 2. Timeline tham khảo

| Milestone GitHub | Người làm xong sẽ demo được gì? | Ước lượng |
|---|---|---:|
| **M1 — Chốt kịch bản giả lập, quyền xem dữ liệu và trạng thái phiếu** | Có ba tình huống, sơ đồ luồng, quyền từng vai trò và tiêu chí chấp nhận | 8–12 giờ |
| **M2 — Đăng nhập, gắn cư dân với căn hộ và chặn truy cập trái quyền** | Ba vai trò đăng nhập; căn hộ xác minh; backend từ chối vượt quyền | 20–35 giờ |
| **M3 — Cư dân tạo, xem phiếu khu vực chung và lưu lịch sử ban đầu** | Cư dân gửi/xem phiếu; quản lý thấy người báo và căn hộ; người khác không đọc được | 25–40 giờ |
| **M4 — Quản lý tiếp nhận, yêu cầu bổ sung, từ chối và phân công phiếu** | Quản lý xử lý hàng đợi; kỹ thuật viên nhận đúng việc; mọi quyết định có lịch sử | 25–40 giờ |
| **M5 — Kỹ thuật viên cập nhật, quản lý xác nhận, cư dân nhận kết quả và phản hồi** | Luồng đầu cuối từ báo cáo tới kết quả và mở lại khi chưa sửa xong | 30–50 giờ |
| **M6 — Quản lý nhập hộ báo cáo ngoài web và ghi liên hệ trả kết quả** | Báo qua điện thoại/chat có phiếu; người báo được liên hệ thủ công theo kịch bản | 20–35 giờ |
| **M7 — Gộp báo trùng và cho người báo theo dõi phiếu chính** | Hai báo cáo cùng sự cố được gộp/tách; người theo dõi không thấy dữ liệu riêng | 18–30 giờ |
| **M8 — Tạm hoãn, chuyển người xử lý và theo dõi hạn/việc tồn** | Quản lý thấy lý do chờ, người chịu trách nhiệm, mốc xử lý và dữ liệu thống kê có thể giải thích | 20–35 giờ |
| **M9 — Deploy demo, dữ liệu giả, tài liệu chạy thử và kịch bản trình bày** | Người khác mở repo và demo có thể tự chạy luồng, xem giới hạn của sản phẩm | 15–25 giờ |

Tổng từ M1 đến M9 khoảng **181–302 giờ**, chưa kể dự phòng cho việc học và sửa sai. Nếu bạn làm khoảng 10 giờ/tuần, hãy chuẩn bị cho nhiều tháng làm việc; đừng tự ép một ngày hoàn thành khi chưa có dữ liệu tốc độ của chính mình. **Sau M5 đã có demo đầu cuối.** M6–M8 tăng độ bao phủ nghiệp vụ; M9 làm bản portfolio dễ xem và dễ chạy. Kiểm thử, phân quyền và tích hợp vẫn nằm trong M2–M8, không đợi M9.

## 3. M1 — Chốt kịch bản giả lập, quyền xem dữ liệu và trạng thái phiếu

**Các issue cần giải quyết:**

- [ ] Ghi 2–3 nguồn công khai có bối cảnh rõ; cập nhật [sổ giả định](ASSUMPTIONS.md). Nguồn từ nơi khác không chứng minh quy trình ở một chung cư cụ thể.
- [ ] Dựng ba ca: đèn hành lang được báo trùng; quản lý nhập hộ cuộc gọi rò nước khu vực chung; đã báo sửa nhưng lỗi còn.
- [ ] Vẽ sơ đồ hành động và người chịu trách nhiệm từ gửi báo cáo đến đóng/mở lại.
- [ ] Chốt ma trận quyền: ai thấy tên/căn hộ người báo, ai sửa phiếu, ai chuyển trạng thái, ai xem lịch sử và ảnh.
- [ ] Chốt state machine tối thiểu và tiêu chí chấp nhận cho từng luồng; ghi riêng điều chưa được người dùng thật xác minh.

**Xong khi:** Kịch bản, ma trận quyền và tiêu chí chấp nhận đủ cụ thể để viết test ở các milestone sau. Không cần phỏng vấn cư dân để hoàn thành portfolio.

## 4. M2 — Đăng nhập, gắn cư dân với căn hộ và chặn truy cập trái quyền

**Các issue cần giải quyết:**

- [ ] Khởi tạo React/TypeScript, Spring Boot, PostgreSQL, Docker Compose, Flyway và CI build/test tối thiểu.
- [ ] Tạo users, buildings, apartments, building memberships và resident–apartment links; seed dữ liệu giả gồm căn 03.
- [ ] Làm login, logout theo cơ chế đã chọn, `/auth/me`, tài khoản không hoạt động và màn hình phân vai.
- [ ] Kiểm tra quyền backend theo role + building; test cư dân gọi API quản lý, nhân viên sai chung cư, tài khoản bị vô hiệu hóa.
- [ ] Hướng dẫn chạy từ clone sạch và dùng secret mẫu; không commit mật khẩu/secrets thật.

**Xong khi:** Đăng nhập bằng các tài khoản demo trên web; API chặn vượt quyền; migration và CI chạy. Đây là một luồng tích hợp có bảo mật và test ngay tại M2.

## 5. M3 — Cư dân tạo, xem phiếu khu vực chung và lưu lịch sử ban đầu

**Các issue cần giải quyết:**

- [ ] Form báo khu vực chung trên điện thoại: vị trí hỏng, mô tả, ảnh tùy chọn; hướng dẫn trường hợp nguy hiểm hoặc thiết bị riêng trong căn hộ.
- [ ] `POST /issues`: backend lấy người báo và căn hộ đã xác minh, lưu căn hộ tại thời điểm báo, tạo mã/status/lịch sử trong cùng transaction; chống gửi lặp.
- [ ] Danh sách/chi tiết phiếu của cư dân, trạng thái rỗng/lỗi; quản lý thấy người báo và căn hộ, cư dân khác không thấy.
- [ ] Nếu làm ảnh ở milestone này, lưu riêng tư, giới hạn file và test quyền xem; nếu ảnh chưa cần cho demo, ghi quyết định hoãn vào issue và làm cùng luồng ảnh ở M5.
- [ ] Test IDOR, dữ liệu client tự gán `reported_by`/căn hộ, form sai, sự cố trong căn hộ và transaction thiếu lịch sử.

**Xong khi:** Cư dân căn 03 gửi báo đèn hành lang, xem được phiếu của mình; quản lý thấy người báo/căn hộ; cư dân khác không đọc được. UI, API, DB, quyền và test cùng hoàn thành.

## 6. M4 — Quản lý tiếp nhận, yêu cầu bổ sung, từ chối và phân công phiếu

**Các issue cần giải quyết:**

- [ ] Hàng đợi quản lý có lọc cơ bản và thông tin đủ để quyết định.
- [ ] Tiếp nhận hoặc từ chối có lý do; yêu cầu bổ sung và cho người báo trả lời.
- [ ] Đặt ưu tiên và phân công một kỹ thuật viên đang hoạt động; kỹ thuật viên chỉ thấy việc được giao.
- [ ] Ghi status events và assignments trong transaction; xử lý hai quản lý thao tác cạnh tranh.
- [ ] Test chuyển trạng thái trái phép, giao cho người sai role/chung cư, lấy việc của kỹ thuật viên khác và lịch sử không nhất quán.

**Xong khi:** Từ phiếu ở M3, quản lý có thể ra quyết định và giao việc trên web; kỹ thuật viên thấy đúng phiếu. Ca sai quyền và xung đột đã được kiểm tra.

## 7. M5 — Kỹ thuật viên cập nhật, quản lý xác nhận, cư dân nhận kết quả và phản hồi

**Các issue cần giải quyết:**

- [ ] Kỹ thuật viên bắt đầu việc, ghi cập nhật/chẩn đoán ngắn, ảnh sau sửa nếu cần và đề xuất hoàn thành.
- [ ] Quản lý xác nhận `RESOLVED` rồi `CLOSED` theo quy tắc; lịch sử giữ đủ các mốc.
- [ ] Cư dân có tài khoản thấy kết quả và thông báo trong ứng dụng; `read_at` chỉ đổi khi mở thông báo.
- [ ] Cư dân báo “vẫn còn lỗi”; quản lý quyết định mở lại cùng phiếu hoặc tạo phiếu liên kết; không xóa kết quả trước.
- [ ] Test kỹ thuật viên bị thu hồi quyền, thiếu mô tả kết quả, thông báo tạo trùng, quyền xem ảnh và mở lại trái trạng thái.

**Xong khi:** Có thể demo trọn hành trình từ M3 đến M5 trên web, gồm một lần sửa xong và một lần cư dân phản hồi. Mỗi bước có kiểm tra quyền và test nghiệp vụ liên quan.

## 8. M6 — Quản lý nhập hộ báo cáo ngoài web và ghi liên hệ trả kết quả

**Các issue cần giải quyết:**

- [ ] Nhập hộ báo cáo từ điện thoại/chat/trực tiếp; lưu `source`, `received_at` và `created_at` riêng; người nhập không bị giả làm người báo.
- [ ] Liên kết tài khoản/căn hộ nếu đã xác minh; thông tin người gọi tự khai phải có nhãn chưa xác minh.
- [ ] Khi phiếu nhập hộ được giải quyết, tạo việc liên hệ; quản lý ghi đã xác nhận, đã thử nhưng chưa gặp hoặc không có kênh liên hệ.
- [ ] Chặn đóng khi chưa có lần liên hệ hoặc lý do không thể liên hệ; phản hồi “vẫn lỗi” qua kênh ngoài được ghi vào phiếu.
- [ ] Test quyền xem thông tin liên hệ, phân biệt “đã thử” và “đã nhận”, mốc nhập hồi tố, việc liên hệ tạo trùng.

**Xong khi:** Demo được cuộc gọi báo rò nước khu vực chung và một lần quản lý báo kết quả thủ công bằng dữ liệu giả. Hệ thống không tuyên bố đã thông báo khi chưa có bằng chứng.

## 9. M7 — Gộp báo trùng và cho người báo theo dõi phiếu chính

**Các issue cần giải quyết:**

- [ ] Gợi ý phiếu có thể trùng theo vị trí/danh mục; quản lý quyết định gộp có lý do và có thể tách lại.
- [ ] Người báo phiếu phụ theo dõi tiến độ công khai của phiếu chính.
- [ ] Chặn vòng liên kết, gộp sai trạng thái, thông báo trùng và lộ tên/căn hộ/ảnh riêng của người khác.
- [ ] Test gộp/tách, lịch sử, quyền xem và các báo cáo chỉ giống nhau về danh mục nhưng là hai lỗi khác nhau.

**Xong khi:** Hai cư dân báo cùng đèn hỏng có một hồ sơ xử lý chính; mỗi người chỉ thấy dữ liệu mình được phép xem. Gộp và tách có dấu vết.

## 10. M8 — Tạm hoãn, chuyển người xử lý và theo dõi hạn/việc tồn

**Các issue cần giải quyết:**

- [ ] Tạm hoãn vì chờ vật tư/nhà thầu/tiếp cận hiện trường, có lý do và mốc cập nhật tiếp; tiếp tục đúng trạng thái.
- [ ] Thu hồi hoặc chuyển người phụ trách; kỹ thuật viên cũ mất quyền cập nhật ngay.
- [ ] Cấu hình mục tiêu thời gian cho dữ liệu demo; lưu lịch sử đổi ưu tiên/hạn và tách tổng thời gian với thời gian chờ.
- [ ] Hàng đợi việc tồn/quá mốc và thống kê cơ bản có thể truy ngược đến phiếu nguồn.
- [ ] Test quyền, cạnh tranh khi chuyển người, tính mốc khi tạm hoãn/mở lại và loại phiếu trùng/từ chối khỏi chỉ số phù hợp.

**Xong khi:** Quản lý có thể giải thích vì sao một phiếu đang chờ, ai phụ trách và con số trên dashboard được tính từ những phiếu nào. Không công bố SLA demo là cam kết của chung cư thật.

## 11. M9 — Deploy demo, dữ liệu giả, tài liệu chạy thử và kịch bản trình bày

**Các issue cần giải quyết:**

- [ ] Đưa frontend, backend và database demo lên môi trường công khai; chạy migration/seed có kiểm soát.
- [ ] Tạo tài khoản và dữ liệu giả cho các luồng M3–M8; không đưa dữ liệu cá nhân thật lên GitHub/demo.
- [ ] Cập nhật README, ERD, API docs, ảnh màn hình, kịch bản demo và giới hạn đã biết.
- [ ] Chạy lại test tự động và demo đầu cuối trên bản deploy từ điện thoại và máy tính; kiểm tra secrets, CORS, quyền ảnh và backup phù hợp với dữ liệu demo.

**Xong khi:** Một người mở repo có thể hiểu và chạy thử CivicFix không cần hướng dẫn riêng. M9 chỉ kiểm tra toàn bộ bản phát hành; không phải nơi bắt đầu sửa các lỗ hổng đã biết từ M2–M8.

## 12. Việc làm ngay

1. Mở [issue M1](https://github.com/Huybao312/CivicFix/issues/1) và [sổ giả định](ASSUMPTIONS.md); chọn ba giả định rủi ro nhất.
2. Viết ba kịch bản mẫu và ma trận quyền. Kết quả này quyết định test nào phải có trong từng issue sau.
3. Sau M1, làm M2 theo lát cắt đăng nhập + căn hộ + quyền; không cần đợi mọi wireframe hoàn hảo.
4. Mỗi lần xong một issue, chạy demo chức năng đó và test hồi quy trước khi chọn issue kế tiếp.

## Tham khảo phương pháp

- [GOV.UK — Core principles of agile](https://www.gov.uk/service-manual/agile-delivery/core-principles-agile): giao hàng theo vòng lặp, học và điều chỉnh liên tục.
- [GOV.UK — Testing your service regularly](https://www.gov.uk/service-manual/technology/quality-assurance-testing-your-service-regularly): kiểm thử trong quá trình phát triển và chạy test tự động thường xuyên.
- [OWASP ASVS](https://asvs.owasp.org/): nguồn yêu cầu kiểm tra bảo mật cho ứng dụng web và API.
