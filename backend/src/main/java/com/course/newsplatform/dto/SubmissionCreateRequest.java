package com.course.newsplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SubmissionCreateRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    private String content;
    private String mediaType;
    private String mediaUrl;
}
