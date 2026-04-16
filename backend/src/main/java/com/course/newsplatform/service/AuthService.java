package com.course.newsplatform.service;

import com.course.newsplatform.dto.*;

public interface AuthService {

    AuthTokenResponse adminLogin(AdminLoginRequest request);

    AuthTokenResponse wechatLogin(WechatLoginRequest request);

    AuthTokenResponse refreshToken(RefreshTokenRequest request);

    UserProfileResponse profile();
}
