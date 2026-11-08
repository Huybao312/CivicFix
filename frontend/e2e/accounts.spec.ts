import { test, expect } from "@playwright/test";
import type { Page, APIRequestContext } from "@playwright/test";
const password = "CivicFix demo passphrase 2026!";
async function login(
  page: Page,
  email = "admin@demo.civicfix.local",
  pass = password,
) {
  await page.goto("/login");
  await page.request.get("/api/auth/csrf");
  const previousSession = (await page.context().cookies()).find(
    (c) => c.name === "JSESSIONID",
  )?.value;
  await page.getByLabel("Email", { exact: true }).fill(email);
  await page.getByLabel("Mật khẩu", { exact: true }).fill(pass);
  await page.getByRole("button", { name: "Đăng nhập", exact: true }).click();
  if (pass !== "incorrect password") {
    await expect
      .poll(
        async () =>
          (await page.context().cookies()).find((c) => c.name === "JSESSIONID")
            ?.value,
      )
      .not.toBe(previousSession);
  }
}
async function tokenFromMail(
  request: APIRequestContext,
  email: string,
  subject = "Lời mời",
) {
  let id = "";
  await expect(async () => {
    const r = await request.get("http://127.0.0.1:18025/api/v1/messages");
    const body = await r.json();
    id =
      body.messages.find(
        (m: { To: { Address: string }[]; Subject: string }) =>
          m.To.some((t) => t.Address === email) && m.Subject.includes(subject),
      )?.ID || "";
    expect(id).toBeTruthy();
  }).toPass({ timeout: 10000 });
  const mail = await (
    await request.get(`http://127.0.0.1:18025/api/v1/message/${id}`)
  ).json();
  return (mail.Text as string).match(/http:\/\/localhost:4173\/[^\s]+/)![0];
}
async function inviteThroughUI(page: Page, email: string) {
  await page.getByRole("button", { name: "Mời cư dân", exact: true }).click();
  const dialog = page.getByRole("dialog");
  await dialog.getByLabel("Tên cư dân").fill("Cư dân thử nghiệm web");
  await dialog.getByLabel("Email nhận lời mời").fill(email);
  await dialog
    .getByLabel("Căn hộ đã xác minh")
    .selectOption({ label: "Căn 04" });
  await dialog.getByLabel("Căn cứ xác minh").selectOption("RESIDENT_LIST");
  await dialog.getByLabel("Tôi đã kiểm tra đúng người và đúng căn hộ.").check();
  await dialog.getByRole("button", { name: "Kiểm tra lời mời" }).click();
  await expect(dialog).toContainText("Căn 04");
  await dialog.getByRole("button", { name: "Xác nhận và gửi" }).click();
  await expect(dialog).not.toBeVisible();
  await expect(
    page.getByText("Lời mời đã được gửi vào hộp thư thử nghiệm."),
  ).toBeVisible();
}

test("admin invites, resident activates, resets password, and loses ended access", async ({
  page,
  browser,
  request,
}) => {
  const email = `web-${Date.now()}@demo.civicfix.local`;
  await login(page);
  await expect(
    page.getByRole("heading", { name: "Tài khoản cư dân", exact: true }),
  ).toBeVisible();
  await expect(
    page.getByRole("button", { name: "Chi tiết resident@demo.civicfix.local" }),
  ).toBeVisible();
  const sessionCookie = (await page.context().cookies()).find(
    (c) => c.name === "JSESSIONID",
  );
  expect(sessionCookie?.httpOnly).toBe(true);
  expect(sessionCookie?.sameSite).toBe("Lax");
  await page.screenshot({
    path: "test-results/m2-admin-desktop.png",
    fullPage: true,
  });
  for (const width of [390, 768]) {
    await page.setViewportSize({ width, height: 900 });
    expect(
      await page.evaluate(
        () => document.documentElement.scrollWidth <= window.innerWidth,
      ),
    ).toBeTruthy();
    await page.screenshot({
      path: `test-results/m2-admin-${width}.png`,
      fullPage: true,
    });
  }
  await page.setViewportSize({ width: 1440, height: 900 });
  await inviteThroughUI(page, email);
  const activation = await tokenFromMail(request, email);
  const residentContext = await browser.newContext({
    viewport: { width: 390, height: 844 },
  });
  const resident = await residentContext.newPage();
  await resident.goto(activation);
  await expect(resident.getByText("Căn 04", { exact: true })).toBeVisible();
  await resident.screenshot({
    path: "test-results/m2-activation-mobile.png",
    fullPage: true,
  });
  await resident.getByLabel("Mật khẩu mới", { exact: true }).fill(password);
  await resident
    .getByLabel("Nhập lại mật khẩu", { exact: true })
    .fill(password);
  await resident
    .getByRole("button", { name: "Kích hoạt tài khoản", exact: true })
    .click();
  await expect(
    resident.getByRole("heading", { name: "Lời mời đã được chấp nhận" }),
  ).toBeVisible();
  await login(resident, email);
  await expect(
    resident.getByRole("heading", { name: "Cư dân", exact: true }),
  ).toBeVisible();
  await expect(resident.getByText("Căn 04", { exact: true })).toBeVisible();
  expect(
    await resident.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBeTruthy();
  await resident.screenshot({
    path: "test-results/m2-resident-mobile.png",
    fullPage: true,
  });
  await resident.getByRole("button", { name: "Đăng xuất" }).click();
  await resident.getByRole("button", { name: "Quên mật khẩu?" }).click();
  await resident.getByLabel("Email", { exact: true }).fill(email);
  await resident.getByRole("button", { name: "Gửi hướng dẫn đặt lại" }).click();
  await expect(
    resident.getByRole("heading", { name: /Nếu email khớp/ }),
  ).toBeVisible();
  const reset = await tokenFromMail(request, email, "Đặt lại mật khẩu");
  await resident.goto(reset);
  const newPassword = "A different private demo passphrase!";
  await resident.getByLabel("Mật khẩu mới", { exact: true }).fill(newPassword);
  await resident
    .getByLabel("Nhập lại mật khẩu", { exact: true })
    .fill(newPassword);
  await resident.getByRole("button", { name: "Lưu mật khẩu mới" }).click();
  await expect(
    resident.getByRole("heading", { name: "Mật khẩu đã được đổi" }),
  ).toBeVisible();
  await login(resident, email, newPassword);
  await expect(
    resident.getByRole("heading", { name: "Cư dân", exact: true }),
  ).toBeVisible();
  await page
    .getByRole("button", { name: "Tài khoản cư dân", exact: true })
    .click();
  await page.getByRole("button", { name: "Làm mới" }).click();
  await page.getByRole("searchbox").fill(email);
  await page.getByRole("button", { name: `Chi tiết ${email}` }).click();
  await page
    .getByRole("button", { name: "Kết thúc quyền", exact: true })
    .click();
  const reason = page.getByRole("dialog", {
    name: "Kết thúc quyền tại chung cư",
  });
  await reason.getByLabel("Lý do").fill("Cư dân demo chuyển đi");
  await reason.getByRole("button", { name: "Xác nhận", exact: true }).click();
  await expect(reason).not.toBeVisible();
  await resident.reload();
  await expect(
    resident.getByRole("heading", { name: "Đăng nhập", exact: true }),
  ).toBeVisible();
  await residentContext.close();
});

test("invalid invite and generic login error are usable on mobile", async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto("/activate#token=invalid-link");
  await expect(page.getByRole("alert")).toContainText(
    "Liên kết không còn dùng được",
  );
  await login(page, "missing@demo.civicfix.local", "incorrect password");
  await expect(page.getByRole("alert")).toContainText(
    "Không thể đăng nhập với thông tin này",
  );
  expect(
    await page.evaluate(
      () => document.documentElement.scrollWidth <= window.innerWidth,
    ),
  ).toBeTruthy();
});

test("recipient can report wrong apartment and admin sees review task", async ({
  page,
  browser,
  request,
}) => {
  const email = `incorrect-${Date.now()}@demo.civicfix.local`;
  await login(page);
  await inviteThroughUI(page, email);
  const activation = await tokenFromMail(request, email);
  const context = await browser.newContext(),
    recipient = await context.newPage();
  await recipient.goto(activation);
  await recipient
    .getByRole("button", { name: "Thông tin căn hộ không đúng?" })
    .click();
  await expect(
    recipient.getByRole("heading", { name: /Đã báo thông tin không đúng/ }),
  ).toBeVisible();
  await page.getByRole("button", { name: "Làm mới" }).click();
  await page.getByRole("searchbox").fill(email);
  await expect(
    page.getByText("Cần kiểm tra lại thông tin căn hộ"),
  ).toBeVisible();
  await context.close();
});

test("each staff role opens only its own workspace", async ({ page }) => {
  for (const [email, title] of [
    ["manager@demo.civicfix.local", "Quản lý phiếu"],
    ["technician@demo.civicfix.local", "Kỹ thuật viên"],
  ]) {
    await login(page, email);
    await expect(
      page.getByRole("heading", { name: title, exact: true }),
    ).toBeVisible();
    await expect(
      page.getByText("Tài khoản và quyền truy cập đã sẵn sàng."),
    ).toBeVisible();
    await page.getByRole("button", { name: "Đăng xuất" }).click();
  }
});
