package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.FenceMember;
import org.apache.ibatis.annotations.*;

@Mapper
public interface FenceMemberMapper extends BaseMapper<FenceMember> {
    @Select("SELECT * FROM geofence_member WHERE id = #{id} FOR UPDATE")
    FenceMember lock(@Param("id") Long id);
}

