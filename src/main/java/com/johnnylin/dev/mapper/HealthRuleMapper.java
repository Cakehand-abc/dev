package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.HealthRule;
import org.apache.ibatis.annotations.*;

@Mapper
public interface HealthRuleMapper extends BaseMapper<HealthRule> {
    @Select("SELECT * FROM health_rule WHERE id = #{id} FOR UPDATE")
    HealthRule lock(@Param("id") Long id);
}

