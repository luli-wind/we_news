package com.course.newsplatform.dto;

import lombok.Data;

@Data
public class VideoQueryRequest extends PageQuery {

    private String keyword;
    private String category;
    private String status;
}
