package com.course.newsplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.course.newsplatform.common.BizException;
import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.common.PageUtils;
import com.course.newsplatform.common.SecurityUtils;
import com.course.newsplatform.dto.NewsQueryRequest;
import com.course.newsplatform.dto.NewsSaveRequest;
import com.course.newsplatform.entity.News;
import com.course.newsplatform.enums.ContentStatus;
import com.course.newsplatform.mapper.NewsMapper;
import com.course.newsplatform.service.LogService;
import com.course.newsplatform.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NewsServiceImpl implements NewsService {

    private final NewsMapper newsMapper;
    private final LogService logService;

    @Override
    public PageResponse<News> page(NewsQueryRequest request, boolean includeAllStatus) {
        LambdaQueryWrapper<News> wrapper = new LambdaQueryWrapper<>();
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            wrapper.and(w -> w.like(News::getTitle, request.getKeyword()).or().like(News::getSummary, request.getKeyword()));
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            wrapper.eq(News::getCategory, request.getCategory());
        }
        if (includeAllStatus) {
            if (request.getStatus() != null && !request.getStatus().isBlank()) {
                wrapper.eq(News::getStatus, request.getStatus());
            }
        } else {
            wrapper.eq(News::getStatus, ContentStatus.PUBLISHED.name());
        }
        wrapper.orderByDesc(News::getPublishedAt).orderByDesc(News::getCreatedAt);

        Page<News> page = newsMapper.selectPage(new Page<>(request.getPage(), request.getPageSize()), wrapper);
        return PageUtils.toPageResponse(page);
    }

    @Override
    public News detail(Long id, boolean includeUnpublished) {
        News news = newsMapper.selectById(id);
        if (news == null) {
            throw new BizException("新闻不存在");
        }
        if (!includeUnpublished && !ContentStatus.PUBLISHED.name().equals(news.getStatus())) {
            throw new BizException("新闻未发布");
        }
        return news;
    }

    @Override
    public Long create(NewsSaveRequest request) {
        News news = new News();
        news.setTitle(request.getTitle());
        news.setSummary(request.getSummary());
        news.setContent(request.getContent());
        news.setCategory(request.getCategory());
        news.setCoverUrl(request.getCoverUrl());
        news.setStatus(normalizeStatus(request.getStatus()));
        news.setAuthorId(SecurityUtils.currentUserId());
        if (ContentStatus.PUBLISHED.name().equals(news.getStatus())) {
            news.setPublishedAt(LocalDateTime.now());
        }
        newsMapper.insert(news);
        logService.operation("news", "create", "创建新闻: " + news.getTitle());
        return news.getId();
    }

    @Override
    public void update(Long id, NewsSaveRequest request) {
        News news = detail(id, true);
        news.setTitle(request.getTitle());
        news.setSummary(request.getSummary());
        news.setContent(request.getContent());
        news.setCategory(request.getCategory());
        news.setCoverUrl(request.getCoverUrl());
        String status = normalizeStatus(request.getStatus());
        news.setStatus(status);
        if (ContentStatus.PUBLISHED.name().equals(status) && news.getPublishedAt() == null) {
            news.setPublishedAt(LocalDateTime.now());
        }
        newsMapper.updateById(news);
        logService.operation("news", "update", "更新新闻: " + id);
    }

    @Override
    public void delete(Long id) {
        if (newsMapper.deleteById(id) == 0) {
            throw new BizException("新闻不存在");
        }
        logService.operation("news", "delete", "删除新闻: " + id);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return ContentStatus.DRAFT.name();
        }
        try {
            return ContentStatus.valueOf(status).name();
        } catch (Exception ex) {
            throw new BizException("非法状态: " + status);
        }
    }
}
