package com.course.newsplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.course.newsplatform.common.BizException;
import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.common.SecurityUtils;
import com.course.newsplatform.dto.PageQuery;
import com.course.newsplatform.dto.SubmissionAuditRequest;
import com.course.newsplatform.dto.SubmissionCreateRequest;
import com.course.newsplatform.entity.News;
import com.course.newsplatform.entity.PostSubmission;
import com.course.newsplatform.enums.ContentStatus;
import com.course.newsplatform.enums.SubmissionStatus;
import com.course.newsplatform.mapper.NewsMapper;
import com.course.newsplatform.mapper.PostSubmissionMapper;
import com.course.newsplatform.service.LogService;
import com.course.newsplatform.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SubmissionServiceImpl implements SubmissionService {

    private final PostSubmissionMapper postSubmissionMapper;
    private final NewsMapper newsMapper;
    private final LogService logService;

    @Override
    public Long create(SubmissionCreateRequest request) {
        PostSubmission submission = new PostSubmission();
        submission.setUserId(SecurityUtils.currentUserId());
        submission.setTitle(request.getTitle());
        submission.setContent(request.getContent());
        submission.setMediaType(request.getMediaType());
        submission.setMediaUrl(request.getMediaUrl());
        submission.setStatus(SubmissionStatus.PENDING.name());
        postSubmissionMapper.insert(submission);

        logService.operation("submission", "create", "用户投稿: " + submission.getTitle());
        return submission.getId();
    }

    @Override
    public PageResponse<PostSubmission> mySubmissions(PageQuery query) {
        Page<PostSubmission> page = postSubmissionMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()),
                new LambdaQueryWrapper<PostSubmission>()
                        .eq(PostSubmission::getUserId, SecurityUtils.currentUserId())
                        .orderByDesc(PostSubmission::getCreatedAt));
        return PageResponse.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public PageResponse<PostSubmission> adminPage(PageQuery query, String status) {
        LambdaQueryWrapper<PostSubmission> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            wrapper.eq(PostSubmission::getStatus, status);
        }
        wrapper.orderByDesc(PostSubmission::getCreatedAt);
        Page<PostSubmission> page = postSubmissionMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()), wrapper);
        return PageResponse.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public void audit(Long id, SubmissionAuditRequest request) {
        PostSubmission submission = postSubmissionMapper.selectById(id);
        if (submission == null) {
            throw new BizException("投稿不存在");
        }
        SubmissionStatus status;
        try {
            status = SubmissionStatus.valueOf(request.getStatus());
        } catch (Exception ex) {
            throw new BizException("审核状态非法");
        }

        if (status == SubmissionStatus.REJECTED
                && (request.getReviewRemark() == null || request.getReviewRemark().isBlank())) {
            throw new BizException("驳回时必须填写驳回原因");
        }

        submission.setStatus(status.name());
        submission.setReviewerId(SecurityUtils.currentUserId());
        submission.setReviewRemark(request.getReviewRemark());
        postSubmissionMapper.updateById(submission);

        logService.audit("submission", id, status.name(), request.getReviewRemark());

        if (status == SubmissionStatus.APPROVED) {
            News news = new News();
            news.setTitle(submission.getTitle());
            news.setSummary(submission.getContent() == null ? null : submission.getContent().substring(0, Math.min(120, submission.getContent().length())));
            news.setContent(submission.getContent() == null ? "" : submission.getContent());
            news.setCoverUrl(submission.getMediaUrl());
            news.setCategory("用户投稿");
            news.setStatus(ContentStatus.PUBLISHED.name());
            news.setAuthorId(submission.getUserId());
            news.setPublishedAt(LocalDateTime.now());
            newsMapper.insert(news);

            submission.setPublishedNewsId(news.getId());
            postSubmissionMapper.updateById(submission);
        }

        logService.operation("submission", "audit", "审核投稿 " + id + " => " + status.name());
    }
}
