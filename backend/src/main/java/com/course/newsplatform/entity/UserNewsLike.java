package com.course.newsplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_news_like")
public class UserNewsLike {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long newsId;
    private LocalDateTime createdAt;
}
