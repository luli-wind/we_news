package com.course.newsplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("news_media")
public class NewsMedia {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long newsId;
    private String mediaType;
    private String url;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
