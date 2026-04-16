package com.course.newsplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.course.newsplatform.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<AuditLog> {
}
