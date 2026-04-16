package com.course.newsplatform.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CommentView {

    private Long id;
    private String bizType;
    private Long bizId;
    private Long userId;
    private String userNickname;
    private String userAvatar;
    private Long parentId;
    private Long rootId;
    private String content;
    private LocalDateTime createdAt;
    private List<CommentView> replies = new ArrayList<>();
}
