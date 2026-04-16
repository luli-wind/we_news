package com.course.newsplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.course.newsplatform.entity.OperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OperationLogMapper extends BaseMapper<OperationLog> {
}
