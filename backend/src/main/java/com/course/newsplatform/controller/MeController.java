package com.course.newsplatform.controller;

import com.course.newsplatform.common.ApiResponse;
import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.dto.CommentView;
import com.course.newsplatform.dto.PageQuery;
import com.course.newsplatform.dto.ProfileUpdateRequest;
import com.course.newsplatform.entity.News;
import com.course.newsplatform.entity.PostSubmission;
import com.course.newsplatform.service.AuthService;
import com.course.newsplatform.service.CommentService;
import com.course.newsplatform.service.FavoriteService;
import com.course.newsplatform.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final AuthService authService;
    private final CommentService commentService;
    private final FavoriteService favoriteService;
    private final SubmissionService submissionService;

    @GetMapping("/profile")
    public ApiResponse<?> profile() {
        return ApiResponse.success(authService.profile());
    }

    @PutMapping("/profile")
    public ApiResponse<?> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return ApiResponse.success(authService.updateProfile(request));
    }

    @GetMapping("/comments")
    public ApiResponse<PageResponse<CommentView>> myComments(PageQuery query) {
        return ApiResponse.success(commentService.myComments(query));
    }

    @GetMapping("/favorites")
    public ApiResponse<PageResponse<News>> myFavorites(PageQuery query) {
        return ApiResponse.success(favoriteService.myFavorites(query));
    }

    @GetMapping("/submissions")
    public ApiResponse<PageResponse<PostSubmission>> mySubmissions(PageQuery query) {
        return ApiResponse.success(submissionService.mySubmissions(query));
    }
}
