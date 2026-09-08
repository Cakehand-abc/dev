package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.Location;
import org.apache.ibatis.annotations.*;

@Mapper
public interface LocationMapper extends BaseMapper<Location> {
    @Select("SELECT * FROM location_point WHERE id = #{id} FOR UPDATE")
    Location lock(@Param("id") Long id);
}

