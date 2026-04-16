package com.course.newsplatform.controller;

import com.course.newsplatform.common.ApiResponse;
import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.dto.CommentCreateRequest;
import com.course.newsplatform.dto.CommentView;
import com.course.newsplatform.dto.PageQuery;
import com.course.newsplatform.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ApiResponse<List<CommentView>> list(@RequestParam String bizType, @RequestParam Long bizId) {
        return ApiResponse.success(commentService.listByBiz(bizType, bizId));
    }

    @PostMapping
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody CommentCreateRequest request) {
        return ApiResponse.success(Map.of("id", commentService.create(request)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        commentService.delete(id);
        return ApiResponse.success();
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @DeleteMapping("/admin/{id}")
    public ApiResponse<Void> auditDelete(@PathVariable Long id) {
        commentService.auditDelete(id);
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<CommentView>> my(PageQuery query) {
        return ApiResponse.success(commentService.myComments(query));
    }
}
