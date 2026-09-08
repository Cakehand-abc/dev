package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.Fence;
import org.apache.ibatis.annotations.*;

@Mapper
public interface FenceMapper extends BaseMapper<Fence> {
    @Select("SELECT * FROM geofence WHERE id = #{id} FOR UPDATE")
    Fence lock(@Param("id") Long id);
}

