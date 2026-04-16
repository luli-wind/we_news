package com.course.newsplatform.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubmissionAuditRequest {

    @NotBlank
    private String status;

    private String reviewRemark;
}
