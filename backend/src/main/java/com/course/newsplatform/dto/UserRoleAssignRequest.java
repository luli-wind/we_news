package com.course.newsplatform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRoleAssignRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Long roleId;
}
