package com.course.newsplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.course.newsplatform.common.ApiResponse;
import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.common.SecurityUtils;
import com.course.newsplatform.dto.NewsQueryRequest;
import com.course.newsplatform.dto.NewsSaveRequest;
import com.course.newsplatform.dto.NewsSyncRequest;
import com.course.newsplatform.dto.NewsSyncResult;
import com.course.newsplatform.entity.News;
import com.course.newsplatform.entity.UserNewsLike;
import com.course.newsplatform.mapper.NewsMapper;
import com.course.newsplatform.mapper.UserNewsLikeMapper;
import com.course.newsplatform.service.NewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;
    private final UserNewsLikeMapper userNewsLikeMapper;
    private final NewsMapper newsMapper;

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

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/api/admin/news/repair-images")
    public ApiResponse<Map<String, Object>> repairImages() {
        return ApiResponse.success(newsService.repairImages());
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/api/admin/news/enrich-content")
    public ApiResponse<Map<String, Object>> enrichContent() {
        return ApiResponse.success(newsService.enrichContent());
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/api/admin/news/sync/juhe")
    public ApiResponse<NewsSyncResult> syncJuhe() {
        return ApiResponse.success(newsService.syncJuheNews());
    }

    // === Like endpoints ===

    @PostMapping("/api/news/{id}/like")
    @Transactional
    public ApiResponse<Map<String, Object>> toggleLike(@PathVariable Long id) {
        Long userId = SecurityUtils.currentUserId();
        UserNewsLike existing = userNewsLikeMapper.selectOne(new LambdaQueryWrapper<UserNewsLike>()
                .eq(UserNewsLike::getUserId, userId)
                .eq(UserNewsLike::getNewsId, id));
        boolean liked;
        if (existing != null) {
            userNewsLikeMapper.deleteById(existing.getId());
            liked = false;
        } else {
            UserNewsLike like = new UserNewsLike();
            like.setUserId(userId); like.setNewsId(id); like.setCreatedAt(LocalDateTime.now());
            userNewsLikeMapper.insert(like);
            liked = true;
        }
        long count = userNewsLikeMapper.selectCount(new LambdaQueryWrapper<UserNewsLike>().eq(UserNewsLike::getNewsId, id));
        newsMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<News>()
                .eq(News::getId, id).set(News::getLikeCount, count));
        return ApiResponse.success(Map.of("liked", liked, "likeCount", count));
    }

    @GetMapping("/api/news/{id}/like-status")
    public ApiResponse<Map<String, Object>> likeStatus(@PathVariable Long id) {
        Long userId = SecurityUtils.currentUserId();
        boolean liked = userNewsLikeMapper.selectCount(new LambdaQueryWrapper<UserNewsLike>()
                .eq(UserNewsLike::getUserId, userId).eq(UserNewsLike::getNewsId, id)) > 0;
        long count = userNewsLikeMapper.selectCount(new LambdaQueryWrapper<UserNewsLike>().eq(UserNewsLike::getNewsId, id));
        return ApiResponse.success(Map.of("liked", liked, "likeCount", count));
    }
}
