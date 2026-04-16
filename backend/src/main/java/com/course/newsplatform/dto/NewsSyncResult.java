package com.course.newsplatform.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class NewsSyncResult {

    private int imported;
    private int skipped;
    private int failed;
    private List<NewsSyncFeedDetail> details = new ArrayList<>();
}
