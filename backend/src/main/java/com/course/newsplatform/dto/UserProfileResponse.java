package com.course.newsplatform.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserProfileResponse {

    private Long id;
    private String nickname;
    private String avatar;
    private List<String> roles;
}
