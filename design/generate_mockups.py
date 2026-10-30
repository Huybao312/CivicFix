"""Generate visual review frames for CivicFix. This is design source, not app UI."""

import json
from collections import Counter
from html import escape
from pathlib import Path

HERE = Path(__file__).parent
data = json.loads((HERE / "civicfix-sample-data.json").read_text())
tickets = data["tickets"]
counts = Counter(t["status"] for t in tickets)
areas = Counter(t["category"] for t in tickets)
assert len(tickets) == 10
assert [counts[s] for s in ("Cần xem", "Chờ bổ sung", "Đang sửa", "Chờ xác nhận")] == [3, 2, 4, 1]

INK = "#1E2925"
MUTED = "#66716B"
TEAL = "#1B695B"
RULE = "#D7DDD8"
PAPER = "#F7F8F5"


def text(x, y, value, size=15, weight=400, fill=INK, extra=""):
    return f'<text x="{x}" y="{y}" font-size="{size}" font-weight="{weight}" fill="{fill}" {extra}>{escape(str(value))}</text>'


def line(x1, y1, x2, y2, color=RULE, width=1):
    return f'<line x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}" stroke="{color}" stroke-width="{width}"/>'


def rect(x, y, w, h, color, radius=0):
    return f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="{radius}" fill="{color}"/>'


def svg(body, label):
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="1440" height="900" viewBox="0 0 1440 900" aria-label="{label}">\n'
            + rect(0, 0, 1440, 900, PAPER)
            + '<g font-family="Arial, Helvetica, sans-serif">\n'
            + body + '\n</g></svg>\n')


def nav(active):
    b = '<g id="Navigation">' + rect(0, 0, 1440, 70, "#FFFFFF") + line(0, 69, 1440, 69)
    b += '<path d="M44 23h19v24H44z M44 35h19 M54 23v24 M63 23l8 7v17h-8" fill="none" stroke="#1B695B" stroke-width="2"/>'
    b += text(84, 45, "CivicFix", 23, 700, extra='letter-spacing="-0.7"')
    for x, label in [(286, "Tổng quan"), (411, "Tiếp nhận"), (526, "Công việc"), (644, "Lịch sử")]:
        b += text(x, 43, label, 15, 700 if active == label else 400, INK if active == label else MUTED)
    b += rect(286 if active == "Tổng quan" else 410, 67, 79 if active == "Tổng quan" else 76, 3, TEAL)
    b += text(1065, 31, "KHÔNG GIAN LÀM VIỆC", 11, 700, MUTED, 'letter-spacing="0.7"')
    b += text(1065, 51, data["workspace"], 15)
    b += line(1267, 17, 1267, 53) + text(1291, 43, "Bảo Ngọc", 15)
    return b + '</g>'


def dossier():
    b = '<g id="Issue-dossier">'
    b += text(802, 254, "Hồ sơ đang chọn", 17, 700)
    b += text(802, 293, "CF-1024  /  BÁO CÁO TỪ WEB", 12, 700, TEAL)
    b += text(802, 332, "Đèn hành lang tầng 3", 27, 700, extra='letter-spacing="-0.5"')
    b += text(802, 359, "Đèn nhấp nháy từ tối qua. Lối đi khá tối.", 15, 400, MUTED)
    b += line(802, 382, 1380, 382)
    b += text(802, 414, "Nơi thiết bị hỏng", 12, 400, MUTED)
    b += text(802, 439, "Hành lang trước căn 03", 15, 700)
    b += text(1112, 414, "Thời điểm nhận", 12, 400, MUTED)
    b += text(1112, 439, "09:42 · 27/10/2026", 15)
    b += line(802, 460, 1380, 460)
    b += text(802, 492, "Người báo", 12, 400, MUTED)
    b += text(802, 517, "Nguyễn Minh Anh", 15, 700)
    b += text(1112, 492, "Căn hộ người báo", 12, 400, MUTED)
    b += text(1112, 517, "03 · Đã xác minh", 15)
    b += line(802, 538, 1380, 538)
    b += text(802, 577, "Quy trình xử lý", 17, 700)
    b += '<circle cx="807" cy="612" r="4" fill="#1B695B"/>' + line(807, 616, 807, 676)
    b += text(829, 617, "Đã ghi nhận báo cáo", 15, 700)
    b += text(829, 642, "Cư dân gửi qua web · 09:42, 27/10/2026", 13, 400, MUTED)
    b += text(829, 672, "Đang chờ quản lý xem xét.", 15, 400, MUTED)
    b += line(802, 713, 1380, 713)
    b += text(802, 746, "Quyết định được lưu vào lịch sử phiếu.", 13, 400, MUTED)
    b += '<rect x="802" y="765" width="202" height="42" rx="3" fill="#FFFFFF" stroke="#89958E"/>'
    b += text(825, 792, "Yêu cầu bổ sung", 14, 700)
    b += rect(1020, 765, 360, 42, INK, 3) + text(1133, 792, "Tiếp nhận phiếu", 14, 700, "#FFFFFF")
    return b + '</g>'


def ledger():
    b = nav("Tiếp nhận") + '<g id="Page-heading">'
    b += text(60, 117, "ĐIỀU PHỐI / 27.10.2026", 11, 700, TEAL, 'letter-spacing="0.7"')
    b += text(60, 158, "Tiếp nhận và điều phối", 30, 700, extra='letter-spacing="-0.7"')
    b += text(60, 186, "Ba báo cáo mới cần xem; các phiếu khác đang tiếp tục được xử lý.", 15, 400, MUTED)
    b += rect(1188, 132, 192, 42, INK, 3) + text(1213, 159, "+ Nhập hộ báo cáo", 14, 700, "#FFFFFF")
    b += line(60, 206, 1380, 206) + '</g>'
    b += '<g id="Issue-ledger">' + text(60, 246, "Sổ sự cố", 17, 700)
    b += text(633, 246, "8 / 10 PHIẾU ĐANG MỞ", 12, 400, MUTED)
    b += line(60, 263, 744, 263)
    for i, ticket in enumerate(tickets[:8]):
        y = 264 + i * 69
        if i == 0:
            b += rect(60, y, 684, 69, "#EAF0EB") + rect(60, y, 3, 69, TEAL)
        b += text(78, y + 18, ticket["id"], 12, 400, MUTED)
        b += text(80, y + 47, ticket["floor"], 23, 700 if i == 0 else 400, TEAL if i == 0 else INK)
        b += text(151, y + 30, ticket["title"], 17, 700)
        b += text(151, y + 52, f'{ticket["location"]} · {ticket["reported"]}', 12, 400, MUTED)
        b += text(633 if ticket["status"] != "Chờ bổ sung" else 604, y + 18, ticket["status"].upper(), 12, 700, TEAL if ticket["status"] == "Cần xem" else MUTED)
        b += line(60, y + 69, 744, y + 69)
    b += text(60, 852, "Xem toàn bộ 10 phiếu", 14, 700, TEAL)
    b += text(223, 852, "→", 17, 400, TEAL)
    b += line(770, 224, 770, 811) + '</g>'
    return svg(b + dossier(), "CivicFix — sổ sự cố hiển thị tám phiếu")


def dashboard():
    b = nav("Tổng quan") + '<g id="Overview-heading">'
    b += text(60, 117, "CHUNG CƯ MINH AN / 27.10.2026", 11, 700, TEAL, 'letter-spacing="0.7"')
    b += text(60, 160, "Tổng quan vận hành", 30, 700, extra='letter-spacing="-0.7"')
    b += text(60, 187, "3 phiếu mới cần quyết định; 1 phiếu đang chờ xác nhận kết quả sửa chữa.", 15, 400, MUTED)
    b += text(1200, 187, "Cập nhật 10:15", 13, 400, MUTED)
    b += line(60, 206, 1380, 206) + '</g>'
    b += '<g id="Status-summary">'
    metrics = [(60, "Đang mở", len(tickets)), (390, "Cần xem", counts["Cần xem"]), (720, "Đang sửa", counts["Đang sửa"]), (1050, "Chờ xác nhận", counts["Chờ xác nhận"])]
    for x, label, n in metrics:
        b += text(x, 237, label, 13, 400, MUTED) + text(x, 281, f"{n:02}", 36, 400, TEAL if label == "Cần xem" else INK)
    for x in (365, 695, 1025): b += line(x, 223, x, 290)
    b += line(60, 309, 1380, 309) + '</g>'
    b += '<g id="Decision-queue">'
    b += text(60, 354, "Cần quyết định", 21, 700)
    b += text(728, 354, "3 phiếu", 13, 400, MUTED)
    b += text(60, 378, "Chọn một phiếu để xem bằng chứng và giao xử lý.", 14, 400, MUTED)
    b += line(60, 398, 900, 398)
    for i, t in enumerate(tickets[:3]):
        y = 398 + i * 84
        b += text(60, y + 28, t["id"], 12, 400, MUTED)
        b += text(155, y + 31, t["title"], 17, 700)
        b += text(155, y + 56, f'{t["location"]} · {t["reported"]}', 13, 400, MUTED)
        b += text(852, y + 40, "→", 20, 400, TEAL)
        b += line(60, y + 84, 900, y + 84)
    b += text(60, 680, "Đang thực hiện", 21, 700)
    b += text(748, 680, "4 phiếu", 13, 400, MUTED)
    b += line(60, 698, 900, 698)
    for i, t in enumerate([tickets[5], tickets[6], tickets[7]]):
        y = 698 + i * 50
        b += text(60, y + 32, t["id"], 12, 400, MUTED)
        b += text(155, y + 32, t["title"], 15, 700)
        b += text(745, y + 32, t["floor"] + " · Đang sửa", 13, 400, MUTED)
        b += line(60, y + 50, 900, y + 50)
    b += text(60, 881, "Xem cả 4 phiếu đang sửa →", 14, 700, TEAL)
    b += '</g>'
    b += line(930, 333, 930, 848)
    b += '<g id="Operations-summary">'
    b += text(960, 354, "Trạng thái phiếu", 21, 700)
    for i, (label, count) in enumerate([("Cần xem",3),("Chờ bổ sung",2),("Đang sửa",4),("Chờ xác nhận",1)]):
        y = 390 + i * 54
        b += text(960, y, label, 14)
        b += rect(1110, y - 12, 205, 10, "#E5EAE5")
        b += rect(1110, y - 12, count * 51, 10, TEAL if label == "Cần xem" else "#85958C")
        b += text(1355, y, count, 14, 700, extra='text-anchor="end"')
    b += line(960, 588, 1380, 588)
    b += text(960, 624, "Khu vực phát sinh", 21, 700)
    for i, (label, count) in enumerate(sorted(areas.items(), key=lambda x: -x[1])):
        y = 660 + i * 38
        b += text(960, y, label, 14) + text(1355, y, count, 14, 700, extra='text-anchor="end"')
        b += line(960, y + 13, 1380, y + 13)
    b += text(960, 842, "1 phiếu chờ xác nhận kết quả sửa chữa →", 14, 700, TEAL)
    b += '</g>'
    return svg(b, "CivicFix — trang tổng quan vận hành")


(HERE / "CivicFix-manager-ledger-dense.svg").write_text(ledger())
(HERE / "CivicFix-manager-dashboard.svg").write_text(dashboard())
print("Created two 1440×900 review frames; status and area counts match 10 sample tickets.")
