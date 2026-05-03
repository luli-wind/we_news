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
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Object>> dashboard() {
        return ApiResponse.success(adminService.dashboard());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ApiResponse<PageResponse<User>> users(PageQuery query) {
        return ApiResponse.success(adminService.users(query));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/roles")
    public ApiResponse<List<Role>> roles() {
        return ApiResponse.success(adminService.roles());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/users/assign-role")
    public ApiResponse<Void> assignRole(@Valid @RequestBody UserRoleAssignRequest request) {
        adminService.assignRole(request);
        return ApiResponse.success();
    }

    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @GetMapping("/logs/operations")
    public ApiResponse<PageResponse<OperationLog>> operationLogs(PageQuery query) {
        return ApiResponse.success(adminService.operationLogs(query));
    }
}
