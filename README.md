# WeNews Course Project

A full-stack course project for a WeChat Mini Program news/video platform.

## Modules

- `backend`: Spring Boot 3 + MyBatis-Plus + Spring Security + JWT
- `admin-web`: Vue3 + Vite + Element Plus admin console
- `miniprogram`: Native WeChat mini program client
- `docs`: API and deployment docs

## Quick Start

### 1) Backend

1. Create MySQL database `wenews`.
2. Execute SQL: `backend/src/main/resources/db/schema.sql`
3. Configure DB/Redis in `backend/src/main/resources/application.yml`
4. Run:

```bash
cd backend
mvn spring-boot:run
```

Default admin account:

- username: `admin`
- password: `Admin@123`

### 2) Admin Web

```bash
cd admin-web
npm install
npm run dev
```

### 3) WeChat Mini Program

Open `miniprogram` in WeChat DevTools and set API base URL in `miniprogram/utils/request.js`.

## Key Features

- News and video list/search/detail
- Favorite, comment, reply-to-comment
- "My" page for user comments/favorites/submissions
- User submission with media
- Admin/Editor role-based management

## Suggested Git Workflow

- `main`: stable release
- `dev`: integration branch
- `feature/*`: feature branches
