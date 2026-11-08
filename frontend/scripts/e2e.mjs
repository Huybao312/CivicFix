import { spawn, spawnSync } from "node:child_process";
import { fileURLToPath } from "node:url";
const root = fileURLToPath(new URL("../../", import.meta.url));
const env = {
  ...process.env,
  CIVICFIX_PROJECT_NAME: "civicfix-e2e",
  POSTGRES_PASSWORD: "local-e2e-only-password",
  CIVICFIX_DB_PORT: "15433",
  CIVICFIX_SMTP_PORT: "11025",
  CIVICFIX_MAIL_UI_PORT: "18025",
  CIVICFIX_PUBLIC_ORIGIN: "http://localhost:4173",
  CIVICFIX_API_TARGET: "http://127.0.0.1:18080",
};
const compose = (args) =>
  spawnSync("docker", ["compose", "--project-name", "civicfix-e2e", ...args], {
    cwd: root,
    env,
    stdio: "inherit",
  });
let code = 1;
try {
  if (compose(["up", "-d", "--wait"]).status !== 0)
    throw new Error("Cannot start isolated E2E services");
  code = await new Promise((resolve) => {
    const child = spawn(
      "npx",
      ["playwright", "test", ...process.argv.slice(2)],
      {
        cwd: fileURLToPath(new URL("../", import.meta.url)),
        env,
        stdio: "inherit",
      },
    );
    child.on("exit", (n) => resolve(n ?? 1));
    child.on("error", () => resolve(1));
  });
} finally {
  compose(["down", "--volumes"]);
}
process.exitCode = code;
