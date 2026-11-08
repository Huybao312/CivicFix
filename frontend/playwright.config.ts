import { defineConfig } from "@playwright/test";
import { fileURLToPath } from "node:url";
export default defineConfig({
  testDir: "./e2e",
  timeout: 60000,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: [["list"], ["html", { open: "never" }]],
  use: {
    baseURL: "http://localhost:4173",
    viewport: { width: 1440, height: 900 },
    browserName: "chromium",
    channel: process.env.CIVICFIX_BROWSER_CHANNEL || undefined,
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  webServer: [
    {
      command:
        "./mvnw --batch-mode spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.arguments=--server.port=18080",
      cwd: fileURLToPath(new URL("../backend/", import.meta.url)),
      url: "http://127.0.0.1:18080/api/auth/csrf",
      timeout: 180000,
      reuseExistingServer: false,
    },
    {
      command: "npm run dev -- --port 4173 --strictPort",
      url: "http://localhost:4173",
      timeout: 30000,
      reuseExistingServer: false,
    },
  ],
});
