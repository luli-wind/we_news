package com.course.newsplatform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.course.newsplatform.common.BizException;
import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.dto.PageQuery;
import com.course.newsplatform.dto.UserRoleAssignRequest;
import com.course.newsplatform.entity.OperationLog;
import com.course.newsplatform.entity.Role;
import com.course.newsplatform.entity.User;
import com.course.newsplatform.entity.UserRole;
import com.course.newsplatform.mapper.OperationLogMapper;
import com.course.newsplatform.mapper.RoleMapper;
import com.course.newsplatform.mapper.UserMapper;
import com.course.newsplatform.mapper.UserRoleMapper;
import com.course.newsplatform.service.AdminService;
import com.course.newsplatform.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final OperationLogMapper operationLogMapper;
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
    public PageResponse<OperationLog> operationLogs(PageQuery query) {
        Page<OperationLog> page = operationLogMapper.selectPage(new Page<>(query.getPage(), query.getPageSize()),
                new LambdaQueryWrapper<OperationLog>().orderByDesc(OperationLog::getCreatedAt));
        return PageResponse.of(page.getCurrent(), page.getSize(), page.getTotal(), page.getRecords());
    }
}
