package com.course.newsplatform.service;

import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.dto.PageQuery;
import com.course.newsplatform.dto.UserRoleAssignRequest;
import com.course.newsplatform.entity.OperationLog;
import com.course.newsplatform.entity.Role;
import com.course.newsplatform.entity.User;

import java.util.List;

public interface AdminService {

    PageResponse<User> users(PageQuery query);

    List<Role> roles();

    void assignRole(UserRoleAssignRequest request);

    PageResponse<OperationLog> operationLogs(PageQuery query);
}
