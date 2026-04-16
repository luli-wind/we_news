package com.course.newsplatform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.util.List;

@Data
public class NewsSyncRequest {

    private List<String> sources;

    @Min(1)
    @Max(100)
    private Integer limitPerFeed;
}
