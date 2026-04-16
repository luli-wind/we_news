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

Rules implemented:

- Favorite dedup by unique index
- Comment soft delete via `is_deleted`
- News/video status lifecycle: `DRAFT/PUBLISHED/REJECTED`
- Submission status lifecycle: `PENDING/APPROVED/REJECTED`
