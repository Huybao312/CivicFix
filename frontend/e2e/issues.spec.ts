import { test, expect } from "@playwright/test";
import type { Page } from "@playwright/test";
const password = "CivicFix demo passphrase 2026!";
const building = "00000000-0000-0000-0000-000000000001";
async function login(page: Page, email = "resident@demo.civicfix.local") {
  await page.goto("/login");
  await page.getByLabel("Email", { exact: true }).fill(email);
  await page.getByLabel("Mật khẩu", { exact: true }).fill(password);
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(
    page.getByRole("heading", {
      name: email.startsWith("manager") ? "Phiếu chung cư" : "Phiếu của tôi",
      exact: true,
    }),
  ).toBeVisible();
}
test("resident reports common-area failure, retries safely, manager reads, another resident is denied", async ({
  page,
  browser,
}) => {
  test.setTimeout(90000);
  await page.setViewportSize({ width: 390, height: 844 });
  await login(page);
  await page.getByRole("link", { name: "Báo sự cố khu vực chung" }).click();
  await expect(page.getByText("Có nguy hiểm tức thời?")).toBeVisible();
  await page.getByLabel("Phạm vi sự cố").selectOption("PRIVATE_APARTMENT");
  await expect(
    page.getByRole("button", { name: "Gửi báo cáo", exact: true }),
  ).toBeDisabled();
  await expect(page.getByRole("alert")).toContainText("thiết bị riêng");
  await page.getByLabel("Phạm vi sự cố").selectOption("COMMON_AREA");
  await page
    .getByLabel("Vị trí xảy ra sự cố", { exact: true })
    .fill("Hành lang tầng 3, gần thang máy B");
  const description = `Đèn chớp liên tục vào buổi tối ${Date.now()}`;
  await page.getByLabel("Mô tả sự cố", { exact: true }).fill(description);
  await page.getByLabel("Loại sự cố").selectOption("ELECTRICAL");
  await page.screenshot({
    path: "test-results/m3-report-mobile.png",
    fullPage: true,
  });
  const commands: string[] = [];
  let first = true;
  await page.route(`**/api/buildings/${building}/issues`, async (route) => {
    if (route.request().method() !== "POST") {
      await route.continue();
      return;
    }
    commands.push(route.request().headers()["idempotency-key"]);
    if (first) {
      first = false;
      await route.fetch();
      await route.abort("failed");
    } else await route.continue();
  });
  await page.getByRole("button", { name: "Gửi báo cáo", exact: true }).click();
  await expect(
    page.getByText("Chưa xác nhận được kết quả gửi.", { exact: false }),
  ).toBeVisible();
  await expect(page.getByLabel("Mô tả sự cố", { exact: true })).toBeDisabled();
  await page
    .getByRole("button", { name: "Gửi lại cùng nội dung", exact: true })
    .click();
  await expect(page.getByRole("heading", { name: /^CF-/ })).toBeVisible();
  expect(commands).toHaveLength(2);
  expect(commands[0]).toBe(commands[1]);
  await expect(page.getByText(description, { exact: true })).toBeVisible();
  await expect(
    page.getByText("Căn 03 · đã xác minh khi báo", { exact: true }),
  ).toBeVisible();
  await expect(page.locator(".issue-history li")).toHaveCount(1);
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= innerWidth,
    ),
  ).toBeTruthy();
  const url = page.url(),
    code = await page.getByRole("heading", { name: /^CF-/ }).innerText();
  await page.screenshot({
    path: "test-results/m3-detail-mobile.png",
    fullPage: true,
  });
  await page.reload();
  await expect(
    page.getByRole("heading", { name: code, exact: true }),
  ).toBeVisible();
  await page.getByRole("link", { name: "← Danh sách phiếu" }).click();
  await page.getByLabel("Tìm phiếu", { exact: true }).fill(code);
  await page.getByRole("button", { name: "Tìm kiếm", exact: true }).click();
  await expect(page.locator(".issue-table tbody tr")).toHaveCount(1);
  await page.screenshot({
    path: "test-results/m3-resident-mobile.png",
    fullPage: true,
  });
  const ctx = await browser.newContext({
      viewport: { width: 1440, height: 900 },
    }),
    manager = await ctx.newPage();
  await login(manager, "manager@demo.civicfix.local");
  await manager.getByLabel("Tìm phiếu", { exact: true }).fill(code);
  await manager.getByRole("button", { name: "Tìm kiếm", exact: true }).click();
  await expect(manager.locator(".issue-table tbody tr")).toHaveCount(1);
  await expect(manager.locator(".issue-table")).toContainText("Cư dân demo");
  await expect(manager.locator(".issue-table")).toContainText("Căn 03");
  await manager.screenshot({
    path: "test-results/m3-manager-desktop.png",
    fullPage: true,
  });
  for (const width of [390, 768]) {
    await manager.setViewportSize({ width, height: 900 });
    expect(
      await manager.evaluate(
        () => document.documentElement.scrollWidth <= innerWidth,
      ),
    ).toBeTruthy();
  }
  await manager.getByRole("link", { name: new RegExp(code) }).click();
  await expect(manager.getByText(description, { exact: true })).toBeVisible();
  // Create a second invited resident through the existing real admin API and Mailpit.
  const admin = await browser.newContext(),
    ap = await admin.newPage();
  await ap.goto("/login");
  await ap
    .getByLabel("Email", { exact: true })
    .fill("admin@demo.civicfix.local");
  await ap.getByLabel("Mật khẩu", { exact: true }).fill(password);
  await ap.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  await expect(
    ap.getByRole("heading", { name: "Tài khoản cư dân", exact: true }),
  ).toBeVisible();
  const email = `private-${Date.now()}@demo.civicfix.local`;
  const csrf = await (await admin.request.get("/api/auth/csrf")).json();
  const apartments = await (
    await admin.request.get(`/api/buildings/${building}/apartments`)
  ).json();
  const apartment = apartments.find(
    (a: { id: string; unit_number: string }) => a.unit_number === "04",
  );
  expect(apartment).toBeTruthy();
  const invite = await admin.request.post(
    `/api/buildings/${building}/invitations`,
    {
      headers: { [csrf.headerName]: csrf.token },
      data: {
        email,
        displayName: "Cư dân khác",
        apartmentId: apartment.id,
        verificationSource: "RESIDENT_LIST",
      },
    },
  );
  expect(invite.ok(), await invite.text()).toBeTruthy();
  const messages = await (
    await admin.request.get("http://127.0.0.1:18025/api/v1/messages")
  ).json();
  const message = messages.messages.find((m: { To: { Address: string }[] }) =>
    m.To.some((t) => t.Address === email),
  );
  const mail = await (
    await admin.request.get(
      `http://127.0.0.1:18025/api/v1/message/${message.ID}`,
    )
  ).json();
  const activation = mail.Text.match(/http:\/\/localhost:4173\/[^\s]+/)[0];
  const stranger = await browser.newContext(),
    sp = await stranger.newPage();
  await sp.goto(activation);
  await sp.getByLabel("Mật khẩu mới", { exact: true }).fill(password);
  await sp.getByLabel("Nhập lại mật khẩu", { exact: true }).fill(password);
  await sp
    .getByRole("button", { name: "Kích hoạt tài khoản", exact: true })
    .click();
  await expect(
    sp.getByRole("heading", { name: "Lời mời đã được chấp nhận" }),
  ).toBeVisible();
  await login(sp, email);
  await sp.goto(url.replace("submitted=1", ""));
  await expect(sp.getByRole("alert")).toContainText("Không tìm thấy phiếu");
  await expect(sp.getByText(description, { exact: true })).not.toBeVisible();
  await ctx.close();
  await admin.close();
  await stranger.close();
});
