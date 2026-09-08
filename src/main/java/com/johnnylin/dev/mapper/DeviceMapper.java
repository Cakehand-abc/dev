package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.Device;
import org.apache.ibatis.annotations.*;

@Mapper
public interface DeviceMapper extends BaseMapper<Device> {
    @Select("SELECT * FROM watch_device WHERE id = #{id} FOR UPDATE")
    Device lock(@Param("id") Long id);
}

