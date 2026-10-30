"""Generate three design-review screens for the role-based CivicFix workflow."""

from html import escape
from pathlib import Path

OUT = Path(__file__).parent
PAPER, WHITE, INK = "#F7F8F5", "#FFFFFF", "#1E2925"
MUTED, RULE, TEAL = "#66716B", "#D7DDD8", "#1B695B"


def t(x, y, value, size=15, weight=400, color=INK, extra=""):
    return f'<text x="{x}" y="{y}" font-size="{size}" font-weight="{weight}" fill="{color}" {extra}>{escape(str(value))}</text>'


def r(x, y, w, h, fill=WHITE, radius=0, stroke=None):
    edge = f' stroke="{stroke}"' if stroke else ""
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{radius}" fill="{fill}"{edge}/>'


def ln(x1, y1, x2, y2, color=RULE):
    return f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{color}" stroke-width="1"/>'


def svg(body, label):
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="1440" height="900" viewBox="0 0 1440 900" aria-label="{escape(label)}">{r(0,0,1440,900,PAPER)}<g font-family="Arial, Helvetica, sans-serif">{body}</g></svg>\n'


def nav(active):
    b = '<g id="Main-navigation">' + r(0, 0, 1440, 70) + ln(0, 69, 1440, 69)
    b += '<path d="M44 23h19v24H44z M44 35h19 M54 23v24 M63 23l8 7v17h-8" fill="none" stroke="#1B695B" stroke-width="2"/>'
    b += t(84, 45, "CivicFix", 23, 700, extra='letter-spacing="-0.7"')
    b += t(286, 43, "Việc cần làm", 15, 700 if active == "work" else 400, INK if active == "work" else MUTED)
    b += t(430, 43, "Tất cả phiếu", 15, 700 if active == "tickets" else 400, INK if active == "tickets" else MUTED)
    b += r(286 if active == "work" else 430, 67, 95 if active == "work" else 91, 3, TEAL)
    b += t(1050, 31, "CHUNG CƯ", 11, 700, MUTED, 'letter-spacing="0.7"')
    b += t(1050, 51, "Minh An", 15)
    b += ln(1218, 17, 1218, 53) + t(1242, 43, "Bảo Ngọc · Quản lý", 15)
    return b + '</g>'


tasks = [
    ("CF-1024", "Đèn hành lang tầng 3", "Hành lang trước căn 03", "09:42 hôm nay"),
    ("CF-1025", "Rò nước ống chung", "Trục ống tầng 2", "08:17 hôm nay"),
    ("CF-1028", "Chuông báo cháy kêu ngắt quãng", "Sảnh tầng trệt", "07:36 hôm nay"),
]


def home():
    b = nav("work") + '<g id="Work-heading">'
    b += t(60, 125, "BAN QUẢN LÝ / 27.10.2026", 11, 700, TEAL, 'letter-spacing="0.7"')
    b += t(60, 166, "Việc cần làm", 30, 700, extra='letter-spacing="-0.7"')
    b += t(60, 194, "5 việc đang chờ bạn xử lý tại chung cư Minh An.", 15, 400, MUTED)
    b += r(1188, 136, 192, 42, INK, 3) + t(1212, 163, "+ Nhập hộ báo cáo", 14, 700, WHITE)
    b += ln(60, 217, 1380, 217) + '</g>'
    b += '<g id="Intake-tasks">' + t(60, 258, "Tiếp nhận báo cáo", 20, 700) + t(1360, 258, "3 việc", 13, 400, MUTED, 'text-anchor="end"')
    b += t(60, 285, "Kiểm tra phạm vi, thông tin và quyết định bước tiếp theo.", 14, 400, MUTED)
    b += ln(60, 303, 1380, 303)
    for i, (code, title, place, time) in enumerate(tasks):
        y = 303 + 77 * i
        b += t(60, y + 29, code, 13, 400, MUTED)
        b += t(188, y + 31, title, 17, 700)
        b += t(188, y + 56, place + " · " + time, 13, 400, MUTED)
        b += t(1360, y + 42, "Xem và quyết định  →", 14, 700, TEAL, 'text-anchor="end"')
        b += ln(60, y + 77, 1380, y + 77)
    b += '</g>'
    b += '<g id="Completion-review">' + t(60, 573, "Duyệt đề xuất hoàn thành", 20, 700)
    b += t(1360, 573, "1 việc", 13, 400, MUTED, 'text-anchor="end"') + ln(60, 591, 1380, 591)
    b += t(60, 624, "CF-1032", 13, 400, MUTED) + t(188, 626, "Đầu đọc thẻ cửa sảnh lỗi", 17, 700)
    b += t(188, 650, "Kỹ thuật viên Hoàng Nam đã gửi kết quả kiểm tra.", 13, 400, MUTED)
    b += t(1360, 636, "Duyệt kết quả  →", 14, 700, TEAL, 'text-anchor="end"') + ln(60, 669, 1380, 669) + '</g>'
    b += '<g id="Contact-task">' + t(60, 712, "Báo kết quả cho người báo", 20, 700)
    b += t(1360, 712, "1 việc", 13, 400, MUTED, 'text-anchor="end"') + ln(60, 730, 1380, 730)
    b += t(60, 763, "CF-1033", 13, 400, MUTED) + t(188, 765, "Đèn báo tủ điện chập chờn", 17, 700)
    b += t(188, 789, "Phiếu nhập qua điện thoại · Đã khắc phục · Chưa ghi nhận liên hệ.", 13, 400, MUTED)
    b += t(1360, 775, "Ghi liên hệ  →", 14, 700, TEAL, 'text-anchor="end"') + ln(60, 808, 1380, 808)
    b += t(60, 851, "Tất cả 10 phiếu đang mở  →", 14, 700, TEAL) + '</g>'
    return svg(b, "Trang đầu ban quản lý: năm việc cần làm")


rows = [
    ("CF-1024", "Đèn hành lang tầng 3", "Hành lang trước căn 03", "Chờ xem xét", "—", "09:42 hôm nay"),
    ("CF-1025", "Rò nước ống chung", "Trục ống tầng 2", "Chờ xem xét", "—", "08:17 hôm nay"),
    ("CF-1028", "Chuông báo cháy kêu ngắt quãng", "Sảnh tầng trệt", "Chờ xem xét", "—", "07:36 hôm nay"),
    ("CF-1026", "Cửa sảnh đóng không kín", "Sảnh chính", "Chờ bổ sung", "—", "19:08 hôm qua"),
    ("CF-1029", "Nắp hộp điện bị lỏng", "Hành lang tầng 4", "Chờ bổ sung", "—", "17:21 hôm qua"),
    ("CF-1027", "Đèn cầu thang tầng 5", "Cầu thang phía đông", "Đang xử lý", "Hoàng Nam", "14:21 hôm qua"),
    ("CF-1030", "Tay vịn hành lang lung lay", "Hành lang tầng 6", "Đang xử lý", "Trần Phúc", "11:04 hôm qua"),
    ("CF-1031", "Gạch hành lang bị bong", "Hành lang tầng 3", "Đang xử lý", "Hoàng Nam", "09:13 hôm qua"),
    ("CF-1032", "Đầu đọc thẻ cửa sảnh lỗi", "Cửa sảnh phía nam", "Đang xử lý", "Hoàng Nam", "08:50 hôm nay"),
    ("CF-1033", "Đèn báo tủ điện chập chờn", "Phòng kỹ thuật tầng 1", "Đã khắc phục", "Trần Phúc", "16:40 25/10"),
]


def all_tickets():
    b = nav("tickets") + '<g id="List-heading">'
    b += t(60, 123, "QUẢN LÝ PHIẾU / CHUNG CƯ MINH AN", 11, 700, TEAL, 'letter-spacing="0.7"')
    b += t(60, 165, "Tất cả phiếu", 30, 700)
    b += t(60, 191, "10 phiếu đang mở. Chọn một hàng để xem và xử lý.", 15, 400, MUTED)
    b += r(1188, 140, 192, 42, INK, 3) + t(1212, 167, "+ Nhập hộ báo cáo", 14, 700, WHITE)
    b += '</g><g id="List-controls">'
    b += r(60, 214, 622, 45, WHITE, 3, RULE) + t(78, 242, "Tìm mã phiếu, sự cố hoặc vị trí", 14, 400, MUTED)
    b += r(698, 214, 233, 45, WHITE, 3, RULE) + t(715, 242, "Trạng thái: Tất cả  ⌄", 14)
    b += r(947, 214, 209, 45, WHITE, 3, RULE) + t(964, 242, "Mới cập nhật trước  ⌄", 14)
    b += '</g><g id="Ticket-list">'
    b += t(60, 300, "Mã", 12, 400, MUTED) + t(170, 300, "Sự cố", 12, 400, MUTED)
    b += t(602, 300, "Vị trí hỏng", 12, 400, MUTED) + t(956, 300, "Trạng thái", 12, 400, MUTED)
    b += t(1130, 300, "Phụ trách", 12, 400, MUTED) + t(1370, 300, "Cập nhật", 12, 400, MUTED, 'text-anchor="end"')
    b += ln(60, 317, 1380, 317)
    for i, (code, title, location, status, owner, updated) in enumerate(rows):
        y = 317 + i * 53
        if i == 0: b += r(60, y+1, 1320, 52, "#EAF0EB")
        b += t(60, y + 33, code, 13, 400, MUTED)
        b += t(170, y + 33, title, 15, 700)
        b += t(602, y + 33, location, 14)
        b += t(956, y + 33, status, 13, 700 if status == "Chờ xem xét" else 400, TEAL if status == "Chờ xem xét" else INK)
        b += t(1130, y + 33, owner, 13, 400, MUTED)
        b += t(1370, y + 33, updated, 13, 400, MUTED, 'text-anchor="end"')
        b += ln(60, y + 53, 1380, y + 53)
    b += t(60, 874, "Hiển thị toàn bộ 10 phiếu đang mở", 13, 400, MUTED) + '</g>'
    return svg(b, "Danh sách toàn chiều rộng với mười phiếu")


def ticket_detail():
    b = nav("tickets") + '<g id="Ticket-heading">'
    b += t(60, 112, "←  Tất cả phiếu  /  CF-1024", 14, 400, TEAL)
    b += t(60, 164, "Đèn hành lang tầng 3", 30, 700)
    b += t(60, 193, "CF-1024  ·  Chờ quản lý xem xét  ·  Báo từ web lúc 09:42, 27/10/2026", 15, 400, MUTED)
    b += ln(60, 215, 1380, 215) + '</g>'
    b += '<g id="Next-step">' + r(60, 244, 1320, 173, "#EAF0EB", 3)
    b += t(84, 280, "Bước tiếp theo", 13, 700, TEAL)
    b += t(84, 313, "Kiểm tra báo cáo và quyết định tiếp nhận", 21, 700)
    b += t(84, 341, "Nếu thuộc khu vực chung và đủ thông tin, tiếp nhận. Việc giao người xử lý là bước kế tiếp.", 14, 400, MUTED)
    b += r(84, 361, 174, 40, INK, 3) + t(108, 387, "Tiếp nhận báo cáo", 14, 700, WHITE)
    b += t(290, 387, "Hỏi bổ sung", 14, 700, TEAL)
    b += t(460, 387, "Đánh dấu trùng", 14, 400, INK)
    b += t(648, 387, "Từ chối", 14, 400, INK) + '</g>'
    b += '<g id="Report-evidence">' + t(60, 469, "Báo cáo sự cố", 20, 700) + ln(60, 490, 888, 490)
    b += t(60, 527, "Vị trí thiết bị hỏng", 13, 400, MUTED) + t(60, 554, "Hành lang trước căn 03, tầng 3", 16, 700)
    b += ln(60, 576, 888, 576)
    b += t(60, 613, "Mô tả của người báo", 13, 400, MUTED)
    b += t(60, 641, "Đèn nhấp nháy từ tối qua. Lối đi khá tối.", 16)
    b += ln(60, 662, 888, 662)
    b += t(60, 699, "Ảnh hiện trường", 13, 400, MUTED)
    b += t(60, 729, "Chưa có ảnh. Vị trí và mô tả đã đủ để quản lý kiểm tra ban đầu.", 14, 400, MUTED)
    b += '</g>'
    b += ln(922, 452, 922, 814)
    b += '<g id="Reporter-and-history">' + t(955, 469, "Người báo", 20, 700) + ln(955, 490, 1380, 490)
    b += t(955, 527, "Họ tên", 13, 400, MUTED) + t(955, 553, "Nguyễn Minh Anh", 15, 700)
    b += t(1173, 527, "Căn hộ đã xác minh", 13, 400, MUTED) + t(1173, 553, "03", 15, 700)
    b += ln(955, 576, 1380, 576)
    b += t(955, 613, "Lịch sử", 20, 700)
    b += '<circle cx="961" cy="645" r="4" fill="#1B695B"/>'
    b += t(981, 650, "09:42 · Cư dân gửi báo cáo", 14, 700)
    b += t(981, 677, "Hệ thống đã ghi nhận; chưa có quyết định tiếp nhận.", 13, 400, MUTED)
    b += ln(955, 701, 1380, 701)
    b += t(955, 742, "Sau khi quyết định, lịch sử sẽ ghi người thực hiện và thời điểm.", 13, 400, MUTED)
    b += '</g>'
    b += t(60, 849, "Phiếu khu vực chung  ·  Thông tin người báo chỉ hiển thị cho người có quyền", 13, 400, MUTED)
    return svg(b, "Chi tiết phiếu với bước tiếp theo rõ ràng")


for filename, content in [
    ("CivicFix-v2-work-queue.svg", home()),
    ("CivicFix-v2-all-tickets.svg", all_tickets()),
    ("CivicFix-v2-ticket-detail.svg", ticket_detail()),
]:
    (OUT / filename).write_text(content)
print("Generated three 1440×900 UX v2 review screens.")
