package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.Elder;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ElderMapper extends BaseMapper<Elder> {
    @Select("SELECT * FROM elder WHERE id = #{id} FOR UPDATE")
    Elder lock(@Param("id") Long id);
}

