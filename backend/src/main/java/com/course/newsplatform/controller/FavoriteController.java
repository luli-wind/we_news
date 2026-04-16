package com.course.newsplatform.controller;

import com.course.newsplatform.common.ApiResponse;
import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.dto.PageQuery;
import com.course.newsplatform.entity.News;
import com.course.newsplatform.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @PostMapping("/{newsId}/toggle")
    public ApiResponse<Map<String, Boolean>> toggle(@PathVariable Long newsId) {
        return ApiResponse.success(Map.of("favorited", favoriteService.toggle(newsId)));
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<News>> myFavorites(PageQuery query) {
        return ApiResponse.success(favoriteService.myFavorites(query));
    }

    @GetMapping("/{newsId}/status")
    public ApiResponse<Map<String, Boolean>> status(@PathVariable Long newsId) {
        return ApiResponse.success(Map.of("favorited", favoriteService.isFavorite(newsId)));
    }
}
