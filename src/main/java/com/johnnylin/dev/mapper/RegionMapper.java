package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.Region;
import org.apache.ibatis.annotations.*;

@Mapper
public interface RegionMapper extends BaseMapper<Region> {
    @Select("SELECT * FROM region WHERE id = #{id} FOR UPDATE")
    Region lock(@Param("id") Long id);
}

