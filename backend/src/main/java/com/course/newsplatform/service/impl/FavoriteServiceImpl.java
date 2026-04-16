package com.course.newsplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.course.newsplatform.common.BizException;
import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.common.SecurityUtils;
import com.course.newsplatform.dto.PageQuery;
import com.course.newsplatform.entity.Favorite;
import com.course.newsplatform.entity.News;
import com.course.newsplatform.mapper.FavoriteMapper;
import com.course.newsplatform.mapper.NewsMapper;
import com.course.newsplatform.service.FavoriteService;
import com.course.newsplatform.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteMapper favoriteMapper;
    private final NewsMapper newsMapper;
    private final LogService logService;

    @Override
    public boolean toggle(Long newsId) {
        Long userId = SecurityUtils.currentUserId();
        News news = newsMapper.selectById(newsId);
        if (news == null) {
            throw new BizException("新闻不存在");
        }

        Favorite old = favoriteMapper.selectOne(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getNewsId, newsId));

        if (old != null) {
            favoriteMapper.deleteById(old.getId());
            logService.operation("favorite", "cancel", "取消收藏新闻: " + newsId);
            return false;
        }

        Favorite favorite = new Favorite();
        favorite.setUserId(userId);
        favorite.setNewsId(newsId);
        favoriteMapper.insert(favorite);
        logService.operation("favorite", "add", "收藏新闻: " + newsId);
        return true;
    }

    @Override
    public PageResponse<News> myFavorites(PageQuery query) {
        Long userId = SecurityUtils.currentUserId();
        Page<Favorite> page = favoriteMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreatedAt));

        List<Long> newsIds = page.getRecords().stream().map(Favorite::getNewsId).toList();
        if (newsIds.isEmpty()) {
            return PageResponse.empty(query.getPage(), query.getPageSize());
        }

        List<News> newsList = newsMapper.selectList(new LambdaQueryWrapper<News>().in(News::getId, newsIds));
        Map<Long, News> map = newsList.stream().collect(Collectors.toMap(News::getId, n -> n));

        List<News> ordered = newsIds.stream().map(map::get).filter(Objects::nonNull).toList();
        return PageResponse.of(page.getCurrent(), page.getSize(), page.getTotal(), ordered);
    }

    @Override
    public boolean isFavorite(Long newsId) {
        Long userId = SecurityUtils.currentUserId();
        return favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getUserId, userId)
                .eq(Favorite::getNewsId, newsId)) > 0;
    }
}
