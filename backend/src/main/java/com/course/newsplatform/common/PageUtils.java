package com.course.newsplatform.common;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public final class PageUtils {

    private PageUtils() {
    }

    public static <T> PageResponse<T> toPageResponse(Page<T> page) {
        return PageResponse.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }
}
