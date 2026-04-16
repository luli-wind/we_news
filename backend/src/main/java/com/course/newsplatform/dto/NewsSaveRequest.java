package com.course.newsplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NewsSaveRequest {

    @NotBlank
    @Size(max = 255)
    private String title;

    @Size(max = 500)
    private String summary;

    @NotBlank
    private String content;

    private String category;
    private String coverUrl;
    private String status;
}
