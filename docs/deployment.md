# Deployment Guide

## 1. Prepare dependencies

- JDK 17
- Maven 3.9+
- MySQL 8
- Redis (optional for extension, config reserved)
- Node.js 18+
- WeChat DevTools

## 2. Backend

1. Create DB: `wenews`
2. Run SQL: `backend/src/main/resources/db/schema.sql`
3. Update DB credentials in `backend/src/main/resources/application.yml`
4. Start backend:

```bash
cd backend
mvn spring-boot:run
```

Swagger URL:

- `http://127.0.0.1:8080/swagger-ui.html`

## 3. Admin Web

```bash
cd admin-web
npm install
npm run dev
```

Default admin:

- `admin / Admin@123`

## 4. WeChat Mini Program

1. Open `miniprogram` in WeChat DevTools
2. Configure API addresses in `miniprogram/utils/config.js`:
   - `develop`:
     - DevTools simulator: use `127.0.0.1/localhost`
     - Real-device debug: use your LAN IP (for this setup: `http://10.27.219.96:8080`)
   - `trial` / `release`: must be deployed HTTPS backend domain
3. Start preview/debug

Real-device debug notes:

- `127.0.0.1` / `localhost` points to the phone itself, not your computer.
- If you ever set `wx.setStorageSync('apiBaseUrl', '...')`, clear it when needed:
  - `wx.removeStorageSync('apiBaseUrl')`

Preview/experience version notes:

- Requests must use HTTPS domain configured in WeChat Mini Program legal request domains.

## 5. Domestic RSS sync (manual)

1. Login admin web with `admin` or `editor`
2. Open **新闻管理**
3. Click **同步国内新闻**
4. Verify mini program can read data from `GET /api/news`

## 6. Suggested Demo Accounts

- Admin: `admin / Admin@123`
- Editor: `editor / Editor@123`
- Mini program user: auto-created via `/api/auth/wechat/login`
