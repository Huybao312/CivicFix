# CivicFix frontend

React/TypeScript cho M2: đăng nhập, lời mời/kích hoạt, khôi phục mật khẩu, quản trị cư dân và màn hình phân vai. Dữ liệu lấy từ API thật; xem [README gốc](../README.md) để chạy Backend/PostgreSQL/Mailpit và dùng tài khoản demo.

```bash
npm ci
npm run dev
npm run lint
npm run build
```

Vite proxy `/api` tới `http://127.0.0.1:8080`. Có thể đổi bằng biến môi trường `CIVICFIX_API_TARGET`.

```bash
npx playwright install chromium
npm run test:e2e
```

E2E tự chạy PostgreSQL/Mailpit/Backend/Vite riêng và dọn môi trường `civicfix-e2e` sau test. Không dùng volume local. Báo cáo nằm trong `playwright-report/`, ảnh/trace trong `test-results/` và bị Git bỏ qua.
