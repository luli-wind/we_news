package com.course.newsplatform.service;

import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.dto.PageQuery;
import com.course.newsplatform.dto.SubmissionAuditRequest;
import com.course.newsplatform.dto.SubmissionCreateRequest;
import com.course.newsplatform.entity.PostSubmission;

public interface SubmissionService {

    Long create(SubmissionCreateRequest request);

    PageResponse<PostSubmission> mySubmissions(PageQuery query);

    PageResponse<PostSubmission> adminPage(PageQuery query, String status);

    void audit(Long id, SubmissionAuditRequest request);
}
