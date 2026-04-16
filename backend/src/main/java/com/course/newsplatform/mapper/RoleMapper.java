package com.course.newsplatform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.course.newsplatform.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("SELECT r.* FROM role r JOIN user_role ur ON ur.role_id = r.id WHERE ur.user_id = #{userId}")
    List<Role> findByUserId(Long userId);
}
