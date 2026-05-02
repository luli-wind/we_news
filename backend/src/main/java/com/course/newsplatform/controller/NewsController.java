package com.course.newsplatform.controller;

import com.course.newsplatform.common.ApiResponse;
import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.dto.NewsQueryRequest;
import com.course.newsplatform.dto.NewsSaveRequest;
import com.course.newsplatform.dto.NewsSyncRequest;
import com.course.newsplatform.dto.NewsSyncResult;
import com.course.newsplatform.entity.News;
import com.course.newsplatform.service.NewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    @GetMapping("/api/news")
    public ApiResponse<PageResponse<News>> page(NewsQueryRequest request) {
        return ApiResponse.success(newsService.page(request, false));
    }

    @GetMapping("/api/news/{id}")
    public ApiResponse<News> detail(@PathVariable Long id) {
        return ApiResponse.success(newsService.detail(id, false));
    }

    @GetMapping("/api/news/sync/refresh")
    public ApiResponse<Map<String, Object>> refresh() {
        long before = newsService.count();
        var result = newsService.syncDomesticNews(null);
        long after = newsService.count();
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("imported", result.getImported());
        data.put("skipped", result.getSkipped());
        data.put("failed", result.getFailed());
        data.put("totalBefore", before);
        data.put("totalAfter", after);
        return ApiResponse.success(data);
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @GetMapping("/api/admin/news")
    public ApiResponse<PageResponse<News>> adminPage(NewsQueryRequest request) {
        return ApiResponse.success(newsService.page(request, true));
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/api/news")
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody NewsSaveRequest request) {
        return ApiResponse.success(Map.of("id", newsService.create(request)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PutMapping("/api/news/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody NewsSaveRequest request) {
        newsService.update(id, request);
        return ApiResponse.success();
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @DeleteMapping("/api/news/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        newsService.delete(id);
        return ApiResponse.success();
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/api/admin/news/sync/domestic")
    public ApiResponse<NewsSyncResult> syncDomestic(@Valid @RequestBody(required = false) NewsSyncRequest request) {
        return ApiResponse.success(newsService.syncDomesticNews(request));
    }
}
