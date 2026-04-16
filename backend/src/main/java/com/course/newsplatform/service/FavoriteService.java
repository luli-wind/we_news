package com.course.newsplatform.service;

import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.dto.PageQuery;
import com.course.newsplatform.entity.Favorite;
import com.course.newsplatform.entity.News;

public interface FavoriteService {

    boolean toggle(Long newsId);

    PageResponse<News> myFavorites(PageQuery query);

    boolean isFavorite(Long newsId);
}
