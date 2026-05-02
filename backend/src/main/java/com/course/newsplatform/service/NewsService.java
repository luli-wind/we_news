package com.course.newsplatform.service;

import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.dto.NewsQueryRequest;
import com.course.newsplatform.dto.NewsSaveRequest;
import com.course.newsplatform.dto.NewsSyncRequest;
import com.course.newsplatform.dto.NewsSyncResult;
import com.course.newsplatform.entity.News;

public interface NewsService {

    PageResponse<News> page(NewsQueryRequest request, boolean includeAllStatus);

    News detail(Long id, boolean includeUnpublished);

    Long create(NewsSaveRequest request);

    void update(Long id, NewsSaveRequest request);

    void delete(Long id);

    NewsSyncResult syncDomesticNews(NewsSyncRequest request);

    long count();
}
