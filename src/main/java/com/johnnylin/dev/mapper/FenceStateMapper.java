package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.FenceState;
import org.apache.ibatis.annotations.*;

@Mapper
public interface FenceStateMapper extends BaseMapper<FenceState> {
    @Select("SELECT * FROM geofence_state WHERE id = #{id} FOR UPDATE")
    FenceState lock(@Param("id") Long id);
}

