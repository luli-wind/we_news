package com.course.newsplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.course.newsplatform.common.BizException;
import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.dto.PageQuery;
import com.course.newsplatform.dto.UserRoleAssignRequest;
import com.course.newsplatform.entity.News;
import com.course.newsplatform.entity.OperationLog;
import com.course.newsplatform.entity.PostSubmission;
import com.course.newsplatform.entity.Role;
import com.course.newsplatform.entity.User;
import com.course.newsplatform.entity.UserRole;
import com.course.newsplatform.entity.Video;
import com.course.newsplatform.enums.ContentStatus;
import com.course.newsplatform.enums.SubmissionStatus;
import com.course.newsplatform.mapper.NewsMapper;
import com.course.newsplatform.mapper.OperationLogMapper;
import com.course.newsplatform.mapper.PostSubmissionMapper;
import com.course.newsplatform.mapper.RoleMapper;
import com.course.newsplatform.mapper.UserMapper;
import com.course.newsplatform.mapper.UserRoleMapper;
import com.course.newsplatform.mapper.VideoMapper;
import com.course.newsplatform.service.AdminService;
import com.course.newsplatform.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final OperationLogMapper operationLogMapper;
    private final NewsMapper newsMapper;
    private final VideoMapper videoMapper;
    private final PostSubmissionMapper postSubmissionMapper;
    private final LogService logService;

    @Override
    public PageResponse<User> users(PageQuery query) {
        Page<User> page = userMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()),
                new LambdaQueryWrapper<User>().orderByDesc(User::getCreatedAt));
        return PageResponse.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public List<Role> roles() {
        return roleMapper.selectList(new LambdaQueryWrapper<>());
    }

    @Override
    public void assignRole(UserRoleAssignRequest request) {
        User user = userMapper.selectById(request.getUserId());
        if (user == null) {
            throw new BizException("用户不存在");
        }
        Role role = roleMapper.selectById(request.getRoleId());
        if (role == null) {
            throw new BizException("角色不存在");
        }

        long count = userRoleMapper.selectCount(new LambdaQueryWrapper<UserRole>()
                .eq(UserRole::getUserId, request.getUserId())
                .eq(UserRole::getRoleId, request.getRoleId()));

        if (count == 0) {
            UserRole relation = new UserRole();
            relation.setUserId(request.getUserId());
            relation.setRoleId(request.getRoleId());
            userRoleMapper.insert(relation);
        }

        logService.operation("rbac", "assign_role", "user=" + request.getUserId() + ", role=" + request.getRoleId());
    }

    @Override
    public Map<String, Object> dashboard() {
        long newsCount = newsMapper.selectCount(
                new LambdaQueryWrapper<News>().eq(News::getStatus, ContentStatus.PUBLISHED.name()));
        long videoCount = videoMapper.selectCount(
                new LambdaQueryWrapper<Video>().eq(Video::getStatus, ContentStatus.PUBLISHED.name()));
        long pendingSubmissions = postSubmissionMapper.selectCount(
                new LambdaQueryWrapper<PostSubmission>().eq(PostSubmission::getStatus, SubmissionStatus.PENDING.name()));
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayOperations = operationLogMapper.selectCount(
                new LambdaQueryWrapper<OperationLog>().ge(OperationLog::getCreatedAt, todayStart));

        List<News> recentNews = newsMapper.selectList(new LambdaQueryWrapper<News>()
                .eq(News::getStatus, ContentStatus.PUBLISHED.name())
                .orderByDesc(News::getPublishedAt)
                .last("LIMIT 5"));

        List<PostSubmission> recentSubmissions = postSubmissionMapper.selectList(new LambdaQueryWrapper<PostSubmission>()
                .eq(PostSubmission::getStatus, SubmissionStatus.PENDING.name())
                .orderByDesc(PostSubmission::getCreatedAt)
                .last("LIMIT 5"));

        Map<String, Long> categoryCounts = newsMapper.selectList(new LambdaQueryWrapper<News>()
                        .eq(News::getStatus, ContentStatus.PUBLISHED.name()))
                .stream()
                .collect(Collectors.groupingBy(n -> n.getCategory() != null ? n.getCategory() : "未分类", LinkedHashMap::new, Collectors.counting()));

        List<Map<String, Object>> categories = new ArrayList<>();
        categoryCounts.forEach((name, count) -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", name);
            item.put("count", count);
            categories.add(item);
        });

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("newsCount", newsCount);
        data.put("videoCount", videoCount);
        data.put("pendingSubmissions", pendingSubmissions);
        data.put("todayOperations", todayOperations);
        data.put("recentNews", recentNews);
        data.put("recentSubmissions", recentSubmissions);
        data.put("categories", categories);
        return data;
    }

    @Override
    public PageResponse<OperationLog> operationLogs(PageQuery query) {
        Page<OperationLog> page = operationLogMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()),
                new LambdaQueryWrapper<OperationLog>().orderByDesc(OperationLog::getCreatedAt));
        return PageResponse.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }
}
