# Roadmap thực hiện CivicFix

**Ngày lập:** 27/10/2026

**Đối tượng:** Người làm dự án full stack cá nhân lần đầu

**Điểm xuất phát:** GitHub repository, README và [PRD](PRD.md) đã có; chưa có ứng dụng chạy được

**Mục tiêu:** Một web responsive có thể demo trọn quy trình, có kiểm thử và triển khai công khai bằng dữ liệu giả

## 1. Cách dùng roadmap

Roadmap này dùng **milestone theo kết quả**, không đặt ngày giao hàng cố định. Ước lượng giờ chỉ để bạn sắp xếp sức học và nhận ra khi nào mình đang sa đà. Sau mỗi milestone, hãy cập nhật ước lượng từ thời gian thực tế. Một milestone chỉ xong khi bạn có thể chỉ vào sản phẩm bàn giao và tự kiểm tra điều kiện hoàn thành của nó.

**Hai điểm đích cần phân biệt:**

1. **Demo đầu cuối:** Cư dân gửi phiếu → quản lý tiếp nhận và giao việc → kỹ thuật viên cập nhật → quản lý xác nhận → cư dân xem kết quả. Đây là mục tiêu đầu tiên.
2. **Portfolio release:** Demo trên cộng thêm các ca quan trọng, bảo mật, kiểm thử, triển khai và tài liệu để người khác tự dùng thử.

**Pilot thật** đòi hỏi một ban quản lý tham gia, đầu mối vận hành, chính sách dữ liệu và kênh khẩn cấp. Hoàn thành portfolio không đồng nghĩa đủ điều kiện pilot. Đừng dùng “MVP” để che sự khác biệt này.

## 2. Timeline tham khảo

| Milestone | Kết quả chính | Công sức tham khảo | Phụ thuộc |
|---|---|---:|---|
| M0 — Khởi tạo | Repo và tài liệu nền | Đã xong | — |
| M1 — Hiểu quy trình | Sơ đồ quy trình hiện tại và giả định được ghi rõ | 12–18 giờ | M0 |
| M2 — Khóa phạm vi demo | Luồng, wireframe, state machine và backlog nhỏ | 12–18 giờ | M1 |
| M3 — Nền kỹ thuật | Web, API, DB chạy local; login và quyền tối thiểu | 18–30 giờ | M2 |
| M4 — Demo đầu cuối | Luồng chính qua ba vai trò, lịch sử đúng | 35–55 giờ | M3 |
| M5 — Ca vận hành quan trọng | Nhập hộ, thiếu thông tin, báo trùng, tạm hoãn, phản hồi | 35–60 giờ | M4 |
| M6 — Chất lượng và bảo mật | Kiểm thử quyền, xung đột, upload, trải nghiệm mobile | 25–40 giờ | M5 |
| M7 — Portfolio release | Deploy, dữ liệu demo, README, video/kịch bản demo | 15–25 giờ | M6 |

Tổng công sức tham khảo sau M0 là **152–246 giờ**, cộng khoảng **25% dự phòng** cho học công nghệ và sửa sai: khoảng **190–310 giờ**. Nếu làm 8 giờ/tuần, đó là khoảng 6–9 tháng; nếu 15 giờ/tuần, khoảng 3–5 tháng. Đây là phạm vi lập kế hoạch, không phải lời hứa. Nếu bạn ít kinh nghiệm với cả React lẫn Spring Boot, hãy lấy phía trên của khoảng ước lượng. Nếu PRD tiếp tục nở, timeline này mất giá trị.

### Nhịp làm việc đề xuất

- Mỗi buổi chọn **một kết quả nhỏ có thể kiểm tra**, ví dụ “cư dân tạo phiếu và thấy nó trong database”, không chọn “làm backend”.
- Cuối mỗi tuần: demo thứ đã chạy được, ghi điều chưa xong, điều đã học và chọn tối đa ba việc tiếp theo.
- Mỗi lần thêm chức năng phải hỏi: nó có giúp hoàn thành luồng đầu cuối hoặc xử lý một rủi ro đã thấy không?
- Khi một việc vượt quá khoảng dự kiến, thu nhỏ việc hoặc ghi rõ điều đang cản trở; đừng âm thầm kéo dài mọi milestone.

## 3. M0 — Khởi tạo dự án (đã hoàn thành)

**Sản phẩm bàn giao:** [README](../README.md), [PRD](PRD.md), repository GitHub.

**Việc còn cần nhớ:** Repository hiện mới có tài liệu. Đừng ghi trong README rằng ứng dụng đã triển khai trước khi thực sự có bản chạy.

## 4. M1 — Hiểu quy trình và kiểm chứng giả định

**Mục tiêu:** Biết một sự cố thật đi từ người phát hiện tới người sửa như thế nào, ai nhập thông tin và thông tin bị mất ở đâu.

**Việc làm:**

- [ ] Viết một trang câu hỏi phỏng vấn tập trung vào *lần sự cố gần nhất*, không hỏi “Anh/chị có thích CivicFix không?”.
- [ ] Trao đổi với khoảng 3–5 cư dân và cố gắng có ít nhất một người từng tiếp nhận/giao/sửa việc. Số người không thay thế chất lượng câu chuyện cụ thể.
- [ ] Vẽ quy trình hiện tại: phát hiện → báo → tiếp nhận → giao → sửa → xác nhận; ghi kênh, người chịu trách nhiệm và thời điểm tại mỗi bước.
- [ ] Ghi ít nhất ba tình huống thực tế: báo trùng, ngoài giờ/khẩn cấp, sửa xong nhưng còn lỗi.
- [ ] Lập bảng `giả định | bằng chứng | quyết định | cần kiểm chứng tiếp`.
- [ ] Cập nhật PRD nếu kết quả mâu thuẫn với thiết kế hiện tại.

**Điều kiện hoàn thành:** Bạn có thể giải thích ai sẽ nhập báo cáo từ điện thoại/chat và người đó có lý do gì để làm việc này. Nếu chưa có cơ hội gặp ban quản lý sau khoảng 18 giờ nỗ lực, ghi rõ **chưa xác minh**, chọn một quy trình mô phỏng hợp lý và tiếp tục làm bản portfolio. Không gọi mô phỏng là kết quả nghiên cứu.

**Sai lầm cần tránh:** Phỏng vấn bạn bè về tính năng mong muốn rồi xem đó là bằng chứng nhu cầu vận hành.

## 5. M2 — Khóa phạm vi của demo đầu cuối

**Mục tiêu:** Chọn một luồng đủ nhỏ để hoàn thành, trước khi khởi tạo hàng chục bảng và endpoint.

**Việc làm:**

- [ ] Viết 5 user story cốt lõi: cư dân tạo/xem phiếu; quản lý tiếp nhận/giao; kỹ thuật viên cập nhật/đề xuất xong; quản lý xác nhận; cư dân xem kết quả.
- [ ] Viết tiêu chí chấp nhận cho mỗi story theo mẫu `Given–When–Then`, gồm ít nhất một trường hợp lỗi/quyền.
- [ ] Chốt trạng thái cần cho demo: `SUBMITTED`, `ACCEPTED`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`. Ghi rõ ai được chuyển từng bước.
- [ ] Vẽ wireframe thô cho form báo cáo, danh sách của quản lý, việc của kỹ thuật viên và trang chi tiết chung. Dùng giấy hoặc công cụ bất kỳ; chưa cần thiết kế đẹp.
- [ ] Vẽ ERD tối thiểu: users, buildings/memberships, issues, assignments, issue_events. Những bảng khác thêm khi luồng cần.
- [ ] Ghi danh sách **để sau demo đầu cuối**: gộp trùng, nhập hộ, `NEEDS_INFO`, `ON_HOLD`, ảnh, thông báo, SLA, dashboard.
- [ ] Chọn kịch bản demo với dữ liệu giả và tên vai trò rõ ràng.

**Điều kiện hoàn thành:** Một người khác đọc wireframe và user stories có thể kể lại toàn bộ hành trình, bao gồm ai quyết định đóng phiếu. Bạn biết chính xác màn hình và API nào cần viết trong M4.

**Sai lầm cần tránh:** Thiết kế đủ chín trạng thái và mọi nhánh ngoại lệ ngay trong bản demo đầu tiên. PRD mô tả sản phẩm đích; backlog demo là phần nhỏ có chủ ý.

## 6. M3 — Nền kỹ thuật chạy được trên máy

**Mục tiêu:** Có một đường đi hoàn chỉnh từ browser đến database, với đăng nhập và quyền cơ bản. Chưa xây chức năng phức tạp.

**Việc làm:**

- [ ] Tạo `frontend/` với React + TypeScript và `backend/` với Spring Boot; dùng PostgreSQL cho dữ liệu.
- [ ] Tạo `docker-compose.yml` cho DB local và Flyway migration đầu tiên.
- [ ] Cấu hình biến môi trường mẫu (`.env.example`), không commit secret thật.
- [ ] Tạo dữ liệu giả cho một chung cư, ba vai trò và vài tài khoản demo.
- [ ] Làm login, lấy tài khoản hiện tại và kiểm tra quyền theo `building_memberships` ở backend.
- [ ] Làm một endpoint đọc nhỏ và hiển thị dữ liệu thật trên frontend để kiểm chứng kết nối.
- [ ] Viết README cách chạy local từ máy mới.

**Điều kiện hoàn thành:** Từ bản clone sạch, làm theo README có thể chạy web, API và DB; login được bằng tài khoản demo; cư dân không gọi được endpoint dành cho quản lý. Migration chạy lặp lại có kiểm soát.

**Sai lầm cần tránh:** Dành nhiều ngày chọn thư viện UI, kiến trúc “chuẩn doanh nghiệp”, CI phức tạp hoặc cấu trúc module quá lớn khi chưa có một phiếu sự cố nào.

## 7. M4 — Demo đầu cuối đầu tiên

**Mục tiêu:** Một người tự thao tác hết hành trình chính trong browser. Đây là milestone quan trọng nhất; nếu dừng dự án giữa chừng, ít nhất bạn vẫn có sản phẩm có thể trình bày.

**Thứ tự triển khai — mỗi bước phải chạy từ UI tới DB:**

1. [ ] Cư dân tạo phiếu với vị trí, mô tả; backend gắn người tạo, chung cư, `SUBMITTED` và sự kiện ban đầu.
2. [ ] Cư dân xem danh sách và chi tiết phiếu của mình; cư dân khác không đọc được qua cách đổi ID.
3. [ ] Quản lý xem hàng đợi, tiếp nhận và phân công kỹ thuật viên; ghi sự kiện và người phụ trách.
4. [ ] Kỹ thuật viên xem “Việc của tôi”, bắt đầu và ghi cập nhật ngắn; người không được giao không sửa được.
5. [ ] Kỹ thuật viên đề xuất hoàn thành; quản lý xác nhận `RESOLVED` rồi đóng; cư dân xem kết quả và dòng thời gian.
6. [ ] Kiểm thử các chuyển trạng thái sai, quyền truy cập sai và transaction tạo phiếu/lịch sử.

**Điều kiện hoàn thành:** Bạn có thể reset dữ liệu demo và thực hiện luồng trên trong một lần, không sửa database thủ công, không gọi Postman để bù cho màn hình thiếu. Lịch sử phản ánh đúng những thao tác đã commit. Web dùng được trên điện thoại ở các màn hình cư dân và kỹ thuật viên.

**Sai lầm cần tránh:** Làm hết backend rồi mới làm frontend. Điều đó giấu lỗi thiết kế luồng đến rất muộn.

## 8. M5 — Các ca vận hành quan trọng

**Mục tiêu:** Biến bản demo tuyến tính thành sản phẩm xử lý được các tình huống mà người dùng dễ gặp.

**Thứ tự ưu tiên:**

1. [ ] Quản lý nhập hộ báo cáo từ điện thoại/chat/trực tiếp; lưu `source`, `received_at`, `created_at` riêng.
2. [ ] Quản lý yêu cầu bổ sung thông tin và cư dân trả lời; từ chối có lý do.
3. [ ] Gợi ý và gộp báo trùng thủ công; người báo phiếu phụ chỉ thấy tiến độ công khai.
4. [ ] Tạm hoãn với lý do và thời điểm cập nhật tiếp; tiếp tục đúng trạng thái.
5. [ ] Cư dân phản hồi vẫn còn lỗi; quản lý mở lại hoặc tạo phiếu liên kết.
6. [ ] Ảnh trước/sau xử lý bằng lưu trữ riêng tư; giới hạn loại và kích thước.
7. [ ] Thông báo trong ứng dụng cho các thay đổi quan trọng; nói rõ người dùng phải mở app để đọc.

**Điều kiện hoàn thành:** Mỗi nhánh có kịch bản demo và ít nhất một test cho rủi ro thật: quyền xem dữ liệu gộp, từ chối thiếu lý do, mở lại không mất lịch sử, lỗi upload không làm lộ ảnh.

**Thứ tự có thể thay đổi:** Nếu nghiên cứu M1 chứng minh báo trùng hiếm nhưng nhập hộ rất thường xuyên, ưu tiên nhập hộ trước như trên. Nếu kỹ thuật viên không dùng ảnh, đừng ép xây upload chỉ để đủ PRD.

## 9. M6 — Chất lượng và bảo mật

**Mục tiêu:** Một người lạ có thể thử sản phẩm mà không dễ làm hỏng dữ liệu hay xem nhầm phiếu của người khác.

**Việc làm:**

- [ ] Kiểm thử phân quyền ở mức đối tượng cho phiếu, lịch sử, ảnh, thông báo và danh sách.
- [ ] Kiểm thử hai quản lý cập nhật cùng phiếu; trả `409` hoặc cơ chế xung đột rõ ràng, không ghi đè âm thầm.
- [ ] Kiểm thử gửi form lặp do mạng yếu; không tạo hai phiếu ngoài ý muốn.
- [ ] Kiểm thử transaction trạng thái + lịch sử + phân công; một phần thất bại thì không để dữ liệu nửa chừng.
- [ ] Kiểm tra validation, lỗi dễ hiểu, giới hạn upload, secret, log và quyền truy cập file.
- [ ] Cho ít nhất một người khác thử tác vụ trên điện thoại; ghi nơi họ mắc kẹt và sửa lỗi nghiêm trọng.
- [ ] Rà soát nội dung về sự cố nguy hiểm; không hiển thị số liên hệ giả ở môi trường công khai.

**Điều kiện hoàn thành:** Toàn bộ kịch bản bắt buộc trong [PRD mục 16](PRD.md#16-kiểm-thử-và-tiêu-chí-chấp-nhận-tổng-thể) đã chạy, lỗi nghiêm trọng đã sửa, không có secret trong Git. Dùng [OWASP ASVS](https://asvs.owasp.org/) như danh sách tham khảo cho xác thực và kiểm soát truy cập; không tự tuyên bố ứng dụng đã đạt chứng nhận bảo mật.

## 10. M7 — Phát hành bản portfolio

**Mục tiêu:** Nhà tuyển dụng hoặc người xem repo có thể hiểu vấn đề, chạy thử và thấy quyết định kỹ thuật của bạn.

**Việc làm:**

- [ ] Deploy frontend, backend và PostgreSQL; tách môi trường demo khỏi dữ liệu cá nhân thật.
- [ ] Tạo tài khoản demo cho từng vai trò, dữ liệu giả có ca bình thường, báo trùng, tạm hoãn và mở lại.
- [ ] Cập nhật README: vấn đề, ảnh màn hình, kiến trúc ngắn, cách chạy local, link demo, tài khoản demo, test và giới hạn.
- [ ] Cung cấp ERD, API docs, kịch bản demo 3–5 phút và danh sách quyết định quan trọng.
- [ ] Chạy lại luồng đầu cuối trên bản deploy từ điện thoại và máy tính.
- [ ] Viết phần “Điều đã kiểm chứng / Giả định còn mở” trung thực. Nếu chưa có đối tác chung cư, ghi rõ đây là mô phỏng portfolio.

**Điều kiện hoàn thành:** Một người chưa từng đọc cuộc trò chuyện này mở repo, hiểu CivicFix giải quyết việc gì, truy cập demo và chạy được một luồng mà không nhắn bạn để xin hướng dẫn.

## 11. Sau portfolio: chỉ làm pilot khi có người vận hành

Pilot cần quyết định về kênh tiếp nhận, người nhập hộ, ca ngoài giờ, đầu mối khẩn cấp, quyền riêng tư, lưu dữ liệu, sao lưu và mục tiêu thời gian. Khi có ban quản lý tham gia, lập kế hoạch pilot riêng cùng họ. Dashboard và SLA chỉ được giới thiệu như chỉ số vận hành sau khi kiểm tra chất lượng dữ liệu. PWA, push, app native hoặc đa chung cư chỉ thêm khi một nhu cầu thật đòi hỏi.

## 12. Việc cần làm ngay trong 5 buổi tiếp theo

| Buổi | Việc cụ thể | Đầu ra nhỏ |
|---|---|---|
| 1 | Tạo `docs/discovery-notes.md`; viết 8–10 câu hỏi về lần sự cố gần nhất và bảng giả định | Bộ câu hỏi và 3 giả định rủi ro nhất |
| 2 | Liên hệ người có thể kể một ca thật; thực hiện hoặc lên lịch cuộc trao đổi đầu tiên | Một câu chuyện sự cố có mốc và kênh báo |
| 3 | Vẽ quy trình hiện tại từ câu chuyện đó; đánh dấu nơi báo trùng, thiếu người phụ trách hoặc phải hỏi lại | Sơ đồ một trang |
| 4 | Viết 5 user story của M2 và kịch bản demo đầu cuối | Backlog nhỏ, có thứ tự |
| 5 | Vẽ bốn wireframe thô; cho một người nhìn thử và kể lại luồng | Ghi chú chỗ họ không hiểu |

Nếu chưa tìm được người phỏng vấn ở buổi 2, tiếp tục các buổi còn lại với giả định được gắn nhãn rõ. Đừng chờ “đủ nghiên cứu” để bắt đầu thiết kế; cũng đừng dùng một bản mockup đẹp làm bằng chứng rằng quy trình vận hành sẽ được chấp nhận.

## 13. Nguyên tắc tự kiểm tra tiến độ

Cuối mỗi milestone, tự trả lời bằng bằng chứng:

1. Có thứ gì chạy được, đọc được hoặc quan sát được không?
2. Một người khác có thể kiểm tra mà không cần mình giải thích miệng không?
3. Điều gì trong PRD đã được chứng minh, điều gì vẫn là giả định?
4. Nếu phải dừng dự án hôm nay, phần đã làm có thể trình bày trung thực không?

Nếu câu trả lời chủ yếu là “tôi đã viết rất nhiều code nhưng chưa demo được”, bạn đang ưu tiên sai. Chuyển lại sang một lát cắt chức năng nhỏ chạy trọn đường đi.

## Tham khảo phương pháp

- [GOV.UK Service Manual — Learning about users and their needs](https://www.gov.uk/service-manual/user-research/start-by-learning-user-needs): tìm hiểu việc người dùng đang làm và vấn đề họ gặp trước khi chốt giải pháp.
- [GOV.UK Service Manual — Developing a roadmap](https://www.gov.uk/service-manual/agile-delivery/developing-a-roadmap): cập nhật roadmap theo điều học được từ nghiên cứu và sử dụng thực tế.
