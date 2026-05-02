package com.course.newsplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("post_submission")
public class PostSubmission {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String title;
    private String content;
    private String mediaType;
    private String mediaUrl;
    private String status;
    private Long reviewerId;
    private String reviewRemark;
    private Long publishedNewsId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
