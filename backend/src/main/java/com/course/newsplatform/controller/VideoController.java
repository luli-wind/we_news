package com.course.newsplatform.controller;

import com.course.newsplatform.common.ApiResponse;
import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.dto.VideoQueryRequest;
import com.course.newsplatform.dto.VideoSaveRequest;
import com.course.newsplatform.entity.Video;
import com.course.newsplatform.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @GetMapping("/api/videos")
    public ApiResponse<PageResponse<Video>> page(VideoQueryRequest request) {
        return ApiResponse.success(videoService.page(request, false));
    }

    @GetMapping("/api/videos/{id}")
    public ApiResponse<Video> detail(@PathVariable Long id) {
        return ApiResponse.success(videoService.detail(id, false));
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @GetMapping("/api/admin/videos")
    public ApiResponse<PageResponse<Video>> adminPage(VideoQueryRequest request) {
        return ApiResponse.success(videoService.page(request, true));
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PostMapping("/api/videos")
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody VideoSaveRequest request) {
        return ApiResponse.success(Map.of("id", videoService.create(request)));
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PutMapping("/api/videos/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody VideoSaveRequest request) {
        videoService.update(id, request);
        return ApiResponse.success();
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @DeleteMapping("/api/videos/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        videoService.delete(id);
        return ApiResponse.success();
    }
}
