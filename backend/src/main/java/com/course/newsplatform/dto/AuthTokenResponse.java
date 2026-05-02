package com.course.newsplatform.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AuthTokenResponse {

    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private Long userId;
    private String nickname;
    private String avatar;
    private List<String> roles;
}
