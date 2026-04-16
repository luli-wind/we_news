package com.course.newsplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentCreateRequest {

    @NotBlank
    private String bizType;

    @NotNull
    private Long bizId;

    private Long parentId;

    @NotBlank
    private String content;
}
