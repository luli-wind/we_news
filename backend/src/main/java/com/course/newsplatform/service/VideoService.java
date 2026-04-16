package com.course.newsplatform.service;

import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.dto.VideoQueryRequest;
import com.course.newsplatform.dto.VideoSaveRequest;
import com.course.newsplatform.entity.Video;

public interface VideoService {

    PageResponse<Video> page(VideoQueryRequest request, boolean includeAllStatus);

    Video detail(Long id, boolean includeUnpublished);

    Long create(VideoSaveRequest request);

    void update(Long id, VideoSaveRequest request);

    void delete(Long id);
}
