package com.course.newsplatform.controller;

import com.course.newsplatform.common.ApiResponse;
import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.dto.PageQuery;
import com.course.newsplatform.dto.SubmissionAuditRequest;
import com.course.newsplatform.dto.SubmissionCreateRequest;
import com.course.newsplatform.entity.PostSubmission;
import com.course.newsplatform.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;

    @PostMapping
    public ApiResponse<Map<String, Long>> create(@Valid @RequestBody SubmissionCreateRequest request) {
        return ApiResponse.success(Map.of("id", submissionService.create(request)));
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<PostSubmission>> my(PageQuery query) {
        return ApiResponse.success(submissionService.mySubmissions(query));
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @GetMapping("/admin")
    public ApiResponse<PageResponse<PostSubmission>> admin(PageQuery query, @RequestParam(required = false) String status) {
        return ApiResponse.success(submissionService.adminPage(query, status));
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @PutMapping("/admin/{id}/audit")
    public ApiResponse<Void> audit(@PathVariable Long id, @Valid @RequestBody SubmissionAuditRequest request) {
        submissionService.audit(id, request);
        return ApiResponse.success();
    }
}
