package com.course.newsplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.course.newsplatform.common.BizException;
import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.common.SecurityUtils;
import com.course.newsplatform.dto.CommentCreateRequest;
import com.course.newsplatform.dto.CommentView;
import com.course.newsplatform.dto.PageQuery;
import com.course.newsplatform.entity.Comment;
import com.course.newsplatform.entity.News;
import com.course.newsplatform.entity.User;
import com.course.newsplatform.entity.Video;
import com.course.newsplatform.mapper.CommentMapper;
import com.course.newsplatform.mapper.NewsMapper;
import com.course.newsplatform.mapper.UserMapper;
import com.course.newsplatform.mapper.VideoMapper;
import com.course.newsplatform.service.CommentService;
import com.course.newsplatform.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final NewsMapper newsMapper;
    private final VideoMapper videoMapper;
    private final LogService logService;

    @Override
    public Long create(CommentCreateRequest request) {
        String bizType = request.getBizType().toUpperCase(Locale.ROOT);
        if (!"NEWS".equals(bizType) && !"VIDEO".equals(bizType)) {
            throw new BizException("bizType 仅支持 NEWS 或 VIDEO");
        }

        Comment comment = new Comment();
        comment.setBizType(bizType);
        comment.setBizId(request.getBizId());
        comment.setUserId(SecurityUtils.currentUserId());
        comment.setContent(HtmlUtils.htmlEscape(request.getContent().trim()));

        if (request.getParentId() != null) {
            Comment parent = commentMapper.selectById(request.getParentId());
            if (parent == null || parent.getIsDeleted() != null && parent.getIsDeleted() == 1) {
                throw new BizException("父评论不存在");
            }
            if (!parent.getBizType().equals(bizType) || !parent.getBizId().equals(request.getBizId())) {
                throw new BizException("父评论不属于当前业务");
            }
            comment.setParentId(parent.getId());
            comment.setRootId(parent.getRootId() == null ? parent.getId() : parent.getRootId());
        }

        commentMapper.insert(comment);
        logService.operation("comment", "create", "新增评论: " + comment.getId());
        return comment.getId();
    }

    @Override
    public List<CommentView> listByBiz(String bizType, Long bizId) {
        List<Comment> comments = commentMapper.selectList(new LambdaQueryWrapper<Comment>()
                .eq(Comment::getBizType, bizType.toUpperCase(Locale.ROOT))
                .eq(Comment::getBizId, bizId)
                .eq(Comment::getIsDeleted, 0)
                .orderByAsc(Comment::getCreatedAt));

        if (comments.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = comments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds)).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<Long, CommentView> viewMap = new LinkedHashMap<>();
        for (Comment item : comments) {
            CommentView view = new CommentView();
            view.setId(item.getId());
            view.setBizType(item.getBizType());
            view.setBizId(item.getBizId());
            view.setUserId(item.getUserId());
            User user = userMap.get(item.getUserId());
            view.setUserNickname(user == null ? "匿名用户" : user.getNickname());
            view.setUserAvatar(user == null ? null : user.getAvatar());
            view.setParentId(item.getParentId());
            view.setRootId(item.getRootId());
            view.setContent(item.getContent());
            view.setCreatedAt(item.getCreatedAt());
            viewMap.put(view.getId(), view);
        }

        List<CommentView> roots = new ArrayList<>();
        for (CommentView view : viewMap.values()) {
            if (view.getParentId() == null) {
                roots.add(view);
            } else {
                CommentView parent = viewMap.get(view.getParentId());
                if (parent != null) {
                    parent.getReplies().add(view);
                }
            }
        }

        return roots;
    }

    @Override
    public PageResponse<CommentView> myComments(PageQuery query) {
        Page<Comment> page = commentMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getUserId, SecurityUtils.currentUserId())
                        .eq(Comment::getIsDeleted, 0)
                        .orderByDesc(Comment::getCreatedAt));

        List<CommentView> list = page.getRecords().stream().map(item -> {
            CommentView view = new CommentView();
            view.setId(item.getId());
            view.setBizType(item.getBizType());
            view.setBizId(item.getBizId());
            view.setUserId(item.getUserId());
            view.setParentId(item.getParentId());
            view.setRootId(item.getRootId());
            view.setContent(item.getContent());
            view.setCreatedAt(item.getCreatedAt());
            return view;
        }).toList();

        return PageResponse.of(page.getCurrent(), page.getSize(), page.getTotal(), list);
    }

    @Override
    public void delete(Long id) {
        Comment comment = commentMapper.selectById(id);
        if (comment == null || (comment.getIsDeleted() != null && comment.getIsDeleted() == 1)) {
            throw new BizException("评论不存在");
        }
        if (!Objects.equals(comment.getUserId(), SecurityUtils.currentUserId())) {
            throw new BizException(4032, "只能删除自己的评论");
        }
        commentMapper.deleteById(id);
        logService.operation("comment", "delete_self", "用户删除评论: " + id);
    }

    @Override
    public void auditDelete(Long id) {
        if (commentMapper.deleteById(id) == 0) {
            throw new BizException("评论不存在");
        }
        logService.operation("comment", "delete_admin", "后台删除评论: " + id);
    }

    @Override
    public Comment getById(Long id) {
        return commentMapper.selectById(id);
    }

    @Override
    public PageResponse<CommentView> adminListAll(String bizType, int pageNum, int pageSize) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getIsDeleted, 0)
                .orderByDesc(Comment::getCreatedAt);
        if (bizType != null && !bizType.isBlank()) {
            wrapper.eq(Comment::getBizType, bizType.toUpperCase(Locale.ROOT));
        }

        Page<Comment> page = commentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);

        Set<Long> userIds = page.getRecords().stream().map(Comment::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, userIds))
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));

        Map<String, Map<Long, String>> titleCache = new HashMap<>();
        for (Comment c : page.getRecords()) {
            titleCache.computeIfAbsent(c.getBizType(), k -> new HashMap<>());
        }
        for (String type : titleCache.keySet()) {
            Set<Long> bizIds = page.getRecords().stream()
                    .filter(c -> type.equals(c.getBizType()))
                    .map(Comment::getBizId).collect(Collectors.toSet());
            if ("NEWS".equals(type)) {
                newsMapper.selectList(new LambdaQueryWrapper<News>().in(News::getId, bizIds))
                        .forEach(n -> titleCache.get(type).put(n.getId(), n.getTitle()));
            } else if ("VIDEO".equals(type)) {
                videoMapper.selectList(new LambdaQueryWrapper<Video>().in(Video::getId, bizIds))
                        .forEach(v -> titleCache.get(type).put(v.getId(), v.getTitle()));
            }
        }

        List<CommentView> list = page.getRecords().stream().map(c -> {
            CommentView view = new CommentView();
            view.setId(c.getId());
            view.setBizType(c.getBizType());
            view.setBizId(c.getBizId());
            view.setUserId(c.getUserId());
            User user = userMap.get(c.getUserId());
            view.setUserNickname(user == null ? "匿名用户" : user.getNickname());
            view.setUserAvatar(user == null ? null : user.getAvatar());
            view.setParentId(c.getParentId());
            view.setRootId(c.getRootId());
            view.setContent(c.getContent());
            view.setCreatedAt(c.getCreatedAt());
            Map<Long, String> typeTitles = titleCache.getOrDefault(c.getBizType(), Collections.emptyMap());
            view.setBizTitle(typeTitles.getOrDefault(c.getBizId(), c.getBizType() + "#" + c.getBizId()));
            return view;
        }).toList();

        return PageResponse.of(page.getCurrent(), page.getSize(), page.getTotal(), list);
    }
}
