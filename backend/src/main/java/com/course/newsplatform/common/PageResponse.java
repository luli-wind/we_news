package com.course.newsplatform.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private long page;
    private long pageSize;
    private long total;
    private List<T> list;

    public static <T> PageResponse<T> of(long page, long pageSize, long total, List<T> list) {
        return new PageResponse<>(page, pageSize, total, list);
    }

    public static <T> PageResponse<T> empty(long page, long pageSize) {
        return new PageResponse<>(page, pageSize, 0, Collections.emptyList());
    }
}
