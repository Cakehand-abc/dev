package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.Binding;
import org.apache.ibatis.annotations.*;

@Mapper
public interface BindingMapper extends BaseMapper<Binding> {
    @Select("SELECT * FROM device_binding WHERE id = #{id} FOR UPDATE")
    Binding lock(@Param("id") Long id);
}

