package com.course.newsplatform.controller.admin;

import com.course.newsplatform.common.ApiResponse;
import com.course.newsplatform.common.PageResponse;
import com.course.newsplatform.dto.PageQuery;
import com.course.newsplatform.dto.UserRoleAssignRequest;
import com.course.newsplatform.entity.OperationLog;
import com.course.newsplatform.entity.Role;
import com.course.newsplatform.entity.User;
import com.course.newsplatform.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/users")
    public ApiResponse<PageResponse<User>> users(PageQuery query) {
        return ApiResponse.success(adminService.users(query));
    }

    @GetMapping("/roles")
    public ApiResponse<List<Role>> roles() {
        return ApiResponse.success(adminService.roles());
    }

    @PostMapping("/users/assign-role")
    public ApiResponse<Void> assignRole(@Valid @RequestBody UserRoleAssignRequest request) {
        adminService.assignRole(request);
        return ApiResponse.success();
    }

    @GetMapping("/logs/operations")
    public ApiResponse<PageResponse<OperationLog>> operationLogs(PageQuery query) {
        return ApiResponse.success(adminService.operationLogs(query));
    }
}
