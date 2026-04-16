package com.course.newsplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NewsSyncFeedDetail {

    private String source;
    private String sourceKey;
    private String feedUrl;
    private int imported;
    private int skipped;
    private int failed;
    private String message;
}
