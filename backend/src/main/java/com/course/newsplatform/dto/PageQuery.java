package com.course.newsplatform.dto;

import lombok.Data;

@Data
public class PageQuery {

    private long page = 1;
    private long pageSize = 10;
}
