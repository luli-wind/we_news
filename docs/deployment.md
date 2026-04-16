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
2. Ensure backend API is reachable (update `miniprogram/utils/request.js` if needed)
3. Start preview/debug

## 5. Suggested Demo Accounts

- Admin: `admin / Admin@123`
- Editor: `editor / Editor@123`
- Mini program user: auto-created via `/api/auth/wechat/login`
