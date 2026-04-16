# Backend API Summary

Base URL: `http://127.0.0.1:8080`

## Auth

- `POST /api/auth/admin/login`
- `POST /api/auth/wechat/login`
- `POST /api/auth/refresh`
- `GET /api/auth/profile`

## News

- `GET /api/news`
- `GET /api/news/{id}`
- `POST /api/news` (ADMIN/EDITOR)
- `PUT /api/news/{id}` (ADMIN/EDITOR)
- `DELETE /api/news/{id}` (ADMIN/EDITOR)
- `GET /api/admin/news` (ADMIN/EDITOR)

## Videos

- `GET /api/videos`
- `GET /api/videos/{id}`
- `POST /api/videos` (ADMIN/EDITOR)
- `PUT /api/videos/{id}` (ADMIN/EDITOR)
- `DELETE /api/videos/{id}` (ADMIN/EDITOR)
- `GET /api/admin/videos` (ADMIN/EDITOR)

## Comments

- `GET /api/comments?bizType=NEWS&bizId=1`
- `POST /api/comments`
- `DELETE /api/comments/{id}`
- `DELETE /api/comments/admin/{id}` (ADMIN/EDITOR)
- `GET /api/comments/me`

## Favorites

- `POST /api/favorites/{newsId}/toggle`
- `GET /api/favorites/{newsId}/status`
- `GET /api/favorites/me`

## Me

- `GET /api/me/profile`
- `GET /api/me/comments`
- `GET /api/me/favorites`
- `GET /api/me/submissions`

## Submissions

- `POST /api/submissions`
- `GET /api/submissions/me`
- `GET /api/submissions/admin` (ADMIN/EDITOR)
- `PUT /api/submissions/admin/{id}/audit` (ADMIN/EDITOR)

## Admin

- `GET /api/admin/users` (ADMIN)
- `GET /api/admin/roles` (ADMIN)
- `POST /api/admin/users/assign-role` (ADMIN)
- `GET /api/admin/logs/operations` (ADMIN)

## Files

- `POST /api/files/upload` (authenticated)

## Unified Response

```json
{
  "code": 0,
  "message": "OK",
  "data": {},
  "timestamp": "2026-04-16T12:00:00"
}
```

Pagination response:

```json
{
  "page": 1,
  "pageSize": 10,
  "total": 100,
  "list": []
}
```
