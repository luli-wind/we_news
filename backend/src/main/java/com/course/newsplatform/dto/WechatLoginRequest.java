package com.course.newsplatform.dto;

import lombok.Data;

@Data
public class WechatLoginRequest {

    private String code;
    private String nickname;
    private String avatar;
}
