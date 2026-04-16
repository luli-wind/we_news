# Database Design Notes

Core tables:

- `user`
- `role`
- `user_role`
- `news`
- `news_media`
- `video`
- `comment` (parent/root self-relation)
- `favorite` (`user_id + news_id` unique)
- `post_submission`
- `audit_log`
- `operation_log`

`news` table extensions for external-source sync:

- `source_name`: upstream source display name (e.g. 人民网/新华网)
- `source_url`: original article URL
- `origin_hash`: dedup key (`SHA-256`) with unique index `uk_news_origin_hash`

Rules implemented:

- Favorite dedup by unique index
- Comment soft delete via `is_deleted`
- News/video status lifecycle: `DRAFT/PUBLISHED/REJECTED`
- Submission status lifecycle: `PENDING/APPROVED/REJECTED`
- External news dedup by unique `origin_hash`
