import { test, expect } from "@playwright/test";
import type { Page } from "@playwright/test";
const building = "00000000-0000-0000-0000-000000000001";
const base = `/api/buildings/${building}/issues`;
const password = "CivicFix demo passphrase 2026!";
async function login(page: Page, role: string) {
  await page.goto("/login");
  await page
    .getByLabel("Email", { exact: true })
    .fill(`${role}@demo.civicfix.local`);
  await page.getByLabel("Mật khẩu", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(
    page.getByRole("heading", {
      name:
        role === "manager"
          ? "Phiếu chung cư"
          : role === "technician"
            ? "Việc của tôi"
            : "Phiếu của tôi",
      exact: true,
    }),
  ).toBeVisible();
}
async function command(page: Page, title: string, text: string) {
  await page.getByRole("button", { name: title, exact: true }).click();
  const dialog = page.getByRole("dialog", { name: title, exact: true });
  await dialog.locator("textarea").fill(text);
  await dialog.getByRole("button", { name: title, exact: true }).click();
  await expect(dialog).not.toBeVisible();
}
test("manager triages, resident replies to notification, technician receives private work view", async ({
  page,
  browser,
}) => {
  test.setTimeout(120000);
  await login(page, "resident");
  const csrf = await (await page.request.get("/api/auth/csrf")).json();
  const response = await page.request.post(base, {
    headers: {
      [csrf.headerName]: csrf.token,
      "Idempotency-Key": crypto.randomUUID(),
    },
    data: {
      scope: "COMMON_AREA",
      locationDescription: "Hành lang tầng 3, thang máy B",
      description: "Đèn hành lang chớp liên tục vào buổi tối, cần kiểm tra.",
      category: "ELECTRICAL",
    },
  });
  expect(response.ok(), await response.text()).toBeTruthy();
  const issue = await response.json();
  const managerContext = await browser.newContext({
      viewport: { width: 1440, height: 1000 },
    }),
    manager = await managerContext.newPage();
  const staleContext = await browser.newContext(),
    stale = await staleContext.newPage();
  await login(manager, "manager");
  await login(stale, "manager");
  const managerUrl = `/issues/${issue.id}?building=${building}&role=MANAGER`;
  await manager.goto(managerUrl);
  await stale.goto(managerUrl);
  await expect(
    manager.getByRole("heading", { name: issue.code, exact: true }),
  ).toBeVisible();
  // Native modal focus, Escape dismissal, and focus return.
  const opener = manager.getByRole("button", {
    name: "Yêu cầu bổ sung",
    exact: true,
  });
  await opener.click();
  let dialog = manager.getByRole("dialog", {
    name: "Yêu cầu bổ sung",
    exact: true,
  });
  const text = dialog.locator("textarea");
  await expect(text).toBeFocused();
  await expect(text).toHaveAttribute("aria-invalid", "false");
  await manager.keyboard.press("Escape");
  await expect(dialog).not.toBeVisible();
  await expect(opener).toBeFocused();
  await opener.click();
  dialog = manager.getByRole("dialog", {
    name: "Yêu cầu bổ sung",
    exact: true,
  });
  await dialog.locator("textarea").fill(" ");
  await manager.keyboard.press("Tab");
  await expect(dialog.locator("textarea")).toHaveAttribute(
    "aria-invalid",
    "true",
  );
  const errorId = await dialog
    .locator("textarea")
    .getAttribute("aria-describedby");
  expect(errorId).toContain("error");
  await expect(dialog.locator(".field-error")).toBeVisible();
  await dialog
    .locator("textarea")
    .fill("Đèn hỏng gần thang máy B hay cửa cầu thang?");
  await expect(dialog.locator("textarea")).toHaveAttribute(
    "aria-invalid",
    "false",
  );
  await manager.screenshot({
    path: "test-results/m4-question-dialog.png",
    fullPage: false,
  });
  await dialog
    .getByRole("button", { name: "Yêu cầu bổ sung", exact: true })
    .click();
  await expect(dialog).not.toBeVisible();
  await expect(manager.locator(".issue-detail-heading")).toContainText(
    "Cần thêm thông tin",
  );
  // The second manager must see the newer version before another decision.
  await stale
    .getByRole("button", { name: "Tiếp nhận phiếu", exact: true })
    .click();
  const staleDialog = stale.getByRole("dialog", {
    name: "Tiếp nhận phiếu",
    exact: true,
  });
  await staleDialog
    .locator("textarea")
    .fill("Đã kiểm tra phạm vi khu vực chung.");
  await staleDialog
    .getByRole("button", { name: "Tiếp nhận phiếu", exact: true })
    .click();
  await expect(staleDialog.getByRole("alert")).toContainText(
    "Phiếu vừa được cập nhật",
  );
  await staleDialog
    .getByRole("button", { name: "Tải lại phiếu", exact: true })
    .click();
  await expect(
    staleDialog.getByRole("button", { name: "Tiếp nhận phiếu", exact: true }),
  ).toBeDisabled();
  await expect(staleDialog.locator("textarea")).toHaveValue(
    "Đã kiểm tra phạm vi khu vực chung.",
  );
  await stale.keyboard.press("Escape");
  await expect(staleDialog).not.toBeVisible();
  // Login/listing leaves notifications unread. Opening the matching issue marks it read.
  await page.reload();
  let notifications = await (
    await page.request.get(`/api/buildings/${building}/notifications`)
  ).json();
  const notification = notifications.find(
    (n: { issue_id: string }) => n.issue_id === issue.id,
  );
  expect(notification.read_at).toBeNull();
  await page
    .getByText(/Thông báo/)
    .first()
    .click();
  await page.getByRole("link", { name: /Quản lý cần bạn bổ sung/ }).click();
  await expect(page.locator(".question-panel")).toContainText(
    "Đèn hỏng gần thang máy B",
  );
  await expect
    .poll(async () => {
      notifications = await (
        await page.request.get(`/api/buildings/${building}/notifications`)
      ).json();
      return notifications.find((n: { id: string }) => n.id === notification.id)
        .read_at;
    })
    .not.toBeNull();
  await page.setViewportSize({ width: 390, height: 844 });
  await page.screenshot({
    path: "test-results/m4-resident-question-mobile.png",
    fullPage: true,
  });
  await command(
    page,
    "Trả lời yêu cầu",
    "Đèn ở ngay phía bên trái cửa thang máy B.",
  );
  await expect(page.locator(".issue-detail-heading")).toContainText(
    "Đã ghi nhận",
  );
  await manager.reload();
  await command(
    manager,
    "Từ chối phiếu",
    "Cần đối chiếu lại vị trí trong phạm vi vận hành.",
  );
  await expect(manager.locator(".issue-detail-heading")).toContainText(
    "Không tiếp nhận",
  );
  await command(
    manager,
    "Khôi phục phiếu",
    "Đã đối chiếu, đây là đèn thuộc khu vực chung.",
  );
  await manager
    .getByRole("button", { name: "Đổi mức ưu tiên", exact: true })
    .click();
  dialog = manager.getByRole("dialog", {
    name: "Đổi mức ưu tiên",
    exact: true,
  });
  await dialog.getByLabel(/Mức ưu tiên/).selectOption("HIGH");
  await dialog.locator("textarea").fill("Ưu tiên kiểm tra trong ca trực.");
  await dialog
    .getByRole("button", { name: "Đổi mức ưu tiên", exact: true })
    .click();
  await expect(dialog).not.toBeVisible();
  await command(
    manager,
    "Tiếp nhận phiếu",
    "Đủ thông tin để kiểm tra đèn khu vực chung.",
  );
  await manager
    .getByRole("button", { name: "Phân công kỹ thuật viên", exact: true })
    .click();
  dialog = manager.getByRole("dialog", {
    name: "Phân công kỹ thuật viên",
    exact: true,
  });
  await dialog
    .getByLabel(/Kỹ thuật viên/)
    .selectOption({ label: "Kỹ thuật viên demo" });
  await dialog
    .locator("textarea")
    .fill("Kiểm tra đèn bên trái cửa thang máy B.");
  await manager.screenshot({
    path: "test-results/m4-assignment-dialog.png",
    fullPage: false,
  });
  await dialog
    .getByRole("button", { name: "Phân công kỹ thuật viên", exact: true })
    .click();
  await expect(dialog).not.toBeVisible();
  await expect(manager.locator(".issue-detail-heading")).toContainText(
    "Đã phân công",
  );
  await manager.getByRole("link", { name: "← Danh sách phiếu" }).click();
  await manager
    .getByLabel("Trạng thái", { exact: true })
    .selectOption("ASSIGNED");
  await manager.getByLabel("Ưu tiên", { exact: true }).selectOption("HIGH");
  await manager.getByRole("button", { name: "Tìm kiếm", exact: true }).click();
  await expect(manager.locator(".issue-table")).toContainText(issue.code);
  await manager.screenshot({
    path: "test-results/m4-manager-queue.png",
    fullPage: true,
  });
  const technicianContext = await browser.newContext({
      viewport: { width: 390, height: 844 },
    }),
    technician = await technicianContext.newPage();
  await login(technician, "technician");
  await technician.getByRole("link", { name: new RegExp(issue.code) }).click();
  await expect(
    technician.getByRole("heading", { name: issue.code, exact: true }),
  ).toBeVisible();
  await expect(
    technician.getByText("Người báo", { exact: true }),
  ).not.toBeVisible();
  await expect(
    technician.getByText("Căn hộ khi báo", { exact: true }),
  ).not.toBeVisible();
  const techData = await (
    await technician.request.get(`${base}/${issue.id}`)
  ).json();
  expect(techData).not.toHaveProperty("reported_by");
  expect(techData).not.toHaveProperty("reporter_name_at_report");
  expect(techData).not.toHaveProperty("apartment_label_at_report");
  await expect(technician.locator(".issue-history")).not.toContainText(
    "Ưu tiên kiểm tra trong ca trực.",
  );
  expect(
    await technician.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
  await technician.screenshot({
    path: "test-results/m4-technician-mobile.png",
    fullPage: true,
  });
  await managerContext.close();
  await staleContext.close();
  await technicianContext.close();
});
