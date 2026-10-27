# Roadmap thực hiện CivicFix

**Ngày lập:** 27/10/2026

**Đối tượng:** Người làm dự án full stack cá nhân lần đầu

**Điểm xuất phát:** GitHub repository, README và [PRD](PRD.md) đã có; chưa có ứng dụng chạy được

**Mục tiêu:** Một web responsive có thể demo trọn quy trình mô phỏng, có kiểm thử và triển khai công khai bằng dữ liệu giả

## 1. Cách dùng roadmap

Roadmap này dùng **milestone theo kết quả**, không đặt ngày giao hàng cố định. Ước lượng giờ chỉ để bạn sắp xếp sức học và nhận ra khi nào mình đang sa đà. Sau mỗi milestone, hãy cập nhật ước lượng từ thời gian thực tế. Một milestone chỉ xong khi bạn có thể chỉ vào sản phẩm bàn giao và tự kiểm tra điều kiện hoàn thành của nó.

**Hai điểm đích cần phân biệt:**

1. **Demo đầu cuối:** Cư dân gửi phiếu → quản lý tiếp nhận và giao việc → kỹ thuật viên cập nhật → quản lý xác nhận → cư dân xem kết quả. Đây là mục tiêu đầu tiên.
2. **Portfolio release:** Demo trên cộng thêm các ca quan trọng, bảo mật, kiểm thử, triển khai và tài liệu để người khác tự dùng thử.

**Pilot thật** đòi hỏi một ban quản lý tham gia, đầu mối vận hành, chính sách dữ liệu và kênh khẩn cấp. Hiện bạn chưa thể xin ý kiến cư dân, nên roadmap portfolio dùng tài liệu công khai và tình huống giả lập. Hoàn thành portfolio không đồng nghĩa đã xác thực nhu cầu hoặc đủ điều kiện pilot.

## 2. Timeline tham khảo

| Milestone | Kết quả chính | Công sức tham khảo | Phụ thuộc |
|---|---|---:|---|
| M0 — Khởi tạo | Repo và tài liệu nền | Đã xong | — |
| M1 — Phân tích và mô phỏng | Sơ đồ quy trình giả định, nguồn tham khảo và kịch bản | 8–12 giờ | M0 |
| M2 — Khóa phạm vi demo | Luồng, wireframe, state machine và backlog nhỏ | 12–18 giờ | M1 |
| M3 — Nền kỹ thuật | Web, API, DB chạy local; login và quyền tối thiểu | 18–30 giờ | M2 |
| M4 — Demo đầu cuối | Luồng chính qua ba vai trò, lịch sử đúng | 35–55 giờ | M3 |
| M5 — Ca vận hành quan trọng | Nhập hộ, thiếu thông tin, báo trùng, tạm hoãn, phản hồi | 35–60 giờ | M4 |
| M6 — Chất lượng và bảo mật | Kiểm thử quyền, xung đột, upload, trải nghiệm mobile | 25–40 giờ | M5 |
| M7 — Portfolio release | Deploy, dữ liệu demo, README, video/kịch bản demo | 15–25 giờ | M6 |

Tổng công sức tham khảo sau M0 là **148–240 giờ**, cộng khoảng **25% dự phòng** cho học công nghệ và sửa sai: khoảng **185–300 giờ**. Nếu làm 8 giờ/tuần, đó là khoảng 6–9 tháng; nếu 15 giờ/tuần, khoảng 3–5 tháng. Đây là phạm vi lập kế hoạch, không phải lời hứa. Nếu bạn ít kinh nghiệm với cả React lẫn Spring Boot, hãy lấy phía trên của khoảng ước lượng. Nếu PRD tiếp tục nở, timeline này mất giá trị.

### Nhịp làm việc đề xuất

- Mỗi buổi chọn **một kết quả nhỏ có thể kiểm tra**, ví dụ “cư dân tạo phiếu và thấy nó trong database”, không chọn “làm backend”.
- Cuối mỗi tuần: demo thứ đã chạy được, ghi điều chưa xong, điều đã học và chọn tối đa ba việc tiếp theo.
- Mỗi lần thêm chức năng phải hỏi: nó có giúp hoàn thành luồng đầu cuối hoặc xử lý một rủi ro đã thấy không?
- Khi một việc vượt quá khoảng dự kiến, thu nhỏ việc hoặc ghi rõ điều đang cản trở; đừng âm thầm kéo dài mọi milestone.

## 3. M0 — Khởi tạo dự án (đã hoàn thành)

**Sản phẩm bàn giao:** [README](../README.md), [PRD](PRD.md), repository GitHub.

**Việc còn cần nhớ:** Repository hiện mới có tài liệu. Đừng ghi trong README rằng ứng dụng đã triển khai trước khi thực sự có bản chạy.

## 4. M1 — Phân tích quy trình và mô phỏng tình huống

**Mục tiêu:** Tạo một mô hình nghiệp vụ có lý do và có thể kiểm thử, đồng thời giữ ranh giới rõ giữa thông tin công khai, suy luận và giả định chưa xác minh.

**Việc làm:**

- [ ] Chọn 2–3 nguồn công khai có thể tham khảo về tiếp nhận và xử lý yêu cầu bảo trì; ghi URL, đơn vị phát hành, bối cảnh và giới hạn áp dụng. Không coi quy trình của nơi khác là quy trình của chung cư bạn sẽ phục vụ.
- [ ] Điền và cập nhật [sổ giả định](ASSUMPTIONS.md): căn cứ, mức chắc chắn, tác động nếu sai và cách xác minh sau này.
- [ ] Dựng ít nhất ba ca giả lập: hai cư dân cùng báo đèn hỏng; quản lý nhập hộ cuộc gọi báo rò nước; sự cố nguy hiểm ngoài giờ hoặc lỗi tái diễn sau khi đóng.
- [ ] Vẽ quy trình giả định: phát hiện → báo → tiếp nhận → giao → sửa → xác nhận; ghi kênh và người chịu trách nhiệm ở mỗi bước.
- [ ] Gắn nhãn từng quyết định trong PRD: **được nguồn hỗ trợ**, **suy luận hợp lý** hoặc **chưa xác minh**.
- [ ] Sửa PRD nếu một nguồn đáng tin cho thấy kịch bản đang tự mâu thuẫn; giữ các câu hỏi dành cho pilot trong danh sách mở.

**Điều kiện hoàn thành:** Có sơ đồ quy trình giả định, ba kịch bản, bảng giả định và lời giải thích vì sao trong bản mô phỏng quản lý nhập báo cáo từ điện thoại/chat. Không cần có người tham gia phỏng vấn. Không gọi tài liệu công khai hoặc kịch bản tự dựng là bằng chứng rằng cư dân sẽ dùng CivicFix.

**Sai lầm cần tránh:** Biến quy trình của một chung cư khác hoặc ý kiến trên mạng thành “nghiên cứu người dùng” của CivicFix.

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

**Điều kiện hoàn thành:** Wireframe và user stories mô tả trọn hành trình, bao gồm ai quyết định đóng phiếu. Bạn có thể dùng chúng để liệt kê chính xác màn hình và API cần viết trong M4 mà không phải tự bổ sung quy tắc bằng lời.

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

1. [ ] Quản lý nhập hộ báo cáo từ điện thoại/chat/trực tiếp; lưu `source`, `received_at`, `created_at` riêng và cách liên hệ người báo nếu có.
2. [ ] Quản lý yêu cầu bổ sung thông tin và cư dân trả lời; từ chối có lý do.
3. [ ] Gợi ý và gộp báo trùng thủ công; người báo phiếu phụ chỉ thấy tiến độ công khai.
4. [ ] Tạm hoãn với lý do và thời điểm cập nhật tiếp; tiếp tục đúng trạng thái.
5. [ ] Cư dân phản hồi vẫn còn lỗi; quản lý mở lại hoặc tạo phiếu liên kết.
6. [ ] Ảnh trước/sau xử lý bằng lưu trữ riêng tư; giới hạn loại và kích thước.
7. [ ] Thông báo trong ứng dụng cho người có tài khoản khi có kết quả; với phiếu nhập hộ chưa liên kết tài khoản, tạo việc liên hệ và ghi lần liên hệ thủ công. Phân biệt đã tạo thông báo, đã đọc và đã xác nhận nhận tin.

**Điều kiện hoàn thành:** Mỗi nhánh có kịch bản demo và ít nhất một test cho rủi ro thật: quyền xem dữ liệu gộp, từ chối thiếu lý do, mở lại không mất lịch sử, phiếu nhập hộ không bị ghi là “đã báo cho cư dân” khi chưa liên hệ, lỗi upload không làm lộ ảnh.

**Thứ tự có thể thay đổi:** M1 chỉ giúp xác định giả định rủi ro, chưa chứng minh tần suất báo trùng hay nhập hộ. Ưu tiên các ca cần để demo luồng đầu cuối; các chức năng chưa phục vụ kịch bản có thể chuyển sau portfolio.

## 9. M6 — Chất lượng và bảo mật

**Mục tiêu:** Một người lạ có thể thử sản phẩm mà không dễ làm hỏng dữ liệu hay xem nhầm phiếu của người khác.

**Việc làm:**

- [ ] Kiểm thử phân quyền ở mức đối tượng cho phiếu, lịch sử, ảnh, thông báo và danh sách.
- [ ] Kiểm thử hai quản lý cập nhật cùng phiếu; trả `409` hoặc cơ chế xung đột rõ ràng, không ghi đè âm thầm.
- [ ] Kiểm thử gửi form lặp do mạng yếu; không tạo hai phiếu ngoài ý muốn.
- [ ] Kiểm thử transaction trạng thái + lịch sử + phân công; một phần thất bại thì không để dữ liệu nửa chừng.
- [ ] Kiểm tra validation, lỗi dễ hiểu, giới hạn upload, secret, log và quyền truy cập file.
- [ ] Tự chạy kịch bản trên điện thoại và bằng bàn phím; nếu có người quen sẵn lòng thử, ghi nơi họ mắc kẹt nhưng không coi họ là đại diện cư dân.
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
- [ ] Viết phần “Điều đã kiểm chứng về kỹ thuật / Giả định sản phẩm còn mở” trung thực; ghi rõ đây là mô phỏng portfolio.

**Điều kiện hoàn thành:** Một người chưa từng đọc cuộc trò chuyện này mở repo, hiểu CivicFix giải quyết việc gì, truy cập demo và chạy được một luồng mà không nhắn bạn để xin hướng dẫn.

## 11. Sau portfolio: chỉ làm pilot khi có người vận hành

Pilot cần quyết định về kênh tiếp nhận, người nhập hộ, ca ngoài giờ, đầu mối khẩn cấp, quyền riêng tư, lưu dữ liệu, sao lưu và mục tiêu thời gian. Khi có ban quản lý tham gia, lập kế hoạch pilot riêng cùng họ. Dashboard và SLA chỉ được giới thiệu như chỉ số vận hành sau khi kiểm tra chất lượng dữ liệu. PWA, push, app native hoặc đa chung cư chỉ thêm khi một nhu cầu thật đòi hỏi.

## 12. Việc cần làm ngay trong 5 buổi tiếp theo

| Buổi | Việc cụ thể | Đầu ra nhỏ |
|---|---|---|
| 1 | Mở [sổ giả định](ASSUMPTIONS.md), chọn 3 giả định rủi ro nhất và ghi lý do | Ba giả định được ưu tiên, chưa gán nhãn “đã xác thực” |
| 2 | Tìm 2–3 tài liệu hoặc ví dụ công khai về cách tiếp nhận và theo dõi yêu cầu bảo trì; ghi nguồn và giới hạn | Danh sách nguồn có chú thích |
| 3 | Dựng ba ca giả lập và vẽ đường đi của mỗi ca; đánh dấu bước phải có người quyết định | Sơ đồ một trang và dữ liệu giả |
| 4 | Viết 5 user story của M2 và kịch bản demo đầu cuối | Backlog nhỏ, có thứ tự |
| 5 | Vẽ bốn wireframe thô; tự đi qua ba vai trò và ghi chỗ thiếu bước hoặc thiếu quyết định | Wireframe và danh sách điểm mơ hồ |

Sau năm buổi, bắt đầu M2 dù còn giả định mở. Giữ danh sách đó trong repo để người xem biết điều gì đã được kiểm thử bằng phần mềm và điều gì vẫn cần người dùng thật xác nhận.

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
