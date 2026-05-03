package com.course.newsplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.course.newsplatform.common.BizException;
import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.common.PageUtils;
import com.course.newsplatform.common.SecurityUtils;
import com.course.newsplatform.dto.VideoQueryRequest;
import com.course.newsplatform.dto.VideoSaveRequest;
import com.course.newsplatform.entity.Video;
import com.course.newsplatform.enums.ContentStatus;
import com.course.newsplatform.mapper.VideoMapper;
import com.course.newsplatform.service.LogService;
import com.course.newsplatform.service.VideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoMapper videoMapper;
    private final LogService logService;

    @Override
    public PageResponse<Video> page(VideoQueryRequest request, boolean includeAllStatus) {
        LambdaQueryWrapper<Video> wrapper = new LambdaQueryWrapper<>();
        if (request.getKeyword() != null && !request.getKeyword().isBlank()) {
            wrapper.and(w -> w.like(Video::getTitle, request.getKeyword()).or().like(Video::getDescription, request.getKeyword()));
        }
        if (request.getCategory() != null && !request.getCategory().isBlank()) {
            wrapper.eq(Video::getCategory, request.getCategory());
        }
        if (includeAllStatus) {
            if (request.getStatus() != null && !request.getStatus().isBlank()) {
                wrapper.eq(Video::getStatus, request.getStatus());
            }
        } else {
            wrapper.eq(Video::getStatus, ContentStatus.PUBLISHED.name());
        }
        wrapper.orderByDesc(Video::getPublishedAt).orderByDesc(Video::getCreatedAt);

        Page<Video> page = videoMapper.selectPage(new Page<>(request.getPage(), request.getPageSize()), wrapper);
        return PageUtils.toPageResponse(page);
    }

    @Override
    public Video detail(Long id, boolean includeUnpublished) {
        Video video = videoMapper.selectById(id);
        if (video == null) {
            throw new BizException("视频不存在");
        }
        if (!includeUnpublished && !ContentStatus.PUBLISHED.name().equals(video.getStatus())) {
            throw new BizException("视频未发布");
        }
        return video;
    }

    @Override
    public Long create(VideoSaveRequest request) {
        Video video = new Video();
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setUrl(request.getUrl());
        video.setCoverUrl(request.getCoverUrl());
        video.setCategory(request.getCategory());
        video.setStatus(normalizeStatus(request.getStatus()));
        video.setAuthorId(SecurityUtils.currentUserId());
        if (ContentStatus.PUBLISHED.name().equals(video.getStatus())) {
            video.setPublishedAt(LocalDateTime.now());
        }
        videoMapper.insert(video);
        logService.operation("video", "create", "创建视频: " + video.getTitle());
        return video.getId();
    }

    @Override
    public void update(Long id, VideoSaveRequest request) {
        Video video = detail(id, true);
        video.setTitle(request.getTitle());
        video.setDescription(request.getDescription());
        video.setUrl(request.getUrl());
        video.setCoverUrl(request.getCoverUrl());
        video.setCategory(request.getCategory());
        String status = normalizeStatus(request.getStatus());
        video.setStatus(status);
        if (ContentStatus.PUBLISHED.name().equals(status) && video.getPublishedAt() == null) {
            video.setPublishedAt(LocalDateTime.now());
        }
        videoMapper.updateById(video);
        logService.operation("video", "update", "更新视频: " + id);
    }

    @Override
    public void delete(Long id) {
        if (videoMapper.deleteById(id) == 0) {
            throw new BizException("视频不存在");
        }
        logService.operation("video", "delete", "删除视频: " + id);
    }

    @Override
    public void incrementPlayCount(Long id) {
        Video video = videoMapper.selectById(id);
        if (video == null) {
            throw new BizException("视频不存在");
        }
        video.setPlayCount((video.getPlayCount() != null ? video.getPlayCount() : 0) + 1);
        videoMapper.updateById(video);
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
