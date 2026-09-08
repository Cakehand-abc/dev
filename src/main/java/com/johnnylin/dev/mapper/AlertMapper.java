package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.Alert;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AlertMapper extends BaseMapper<Alert> {
    @Select("SELECT * FROM geofence_alert WHERE id = #{id} FOR UPDATE")
    Alert lock(@Param("id") Long id);
}

