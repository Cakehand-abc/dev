package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.FollowupPlan;
import org.apache.ibatis.annotations.*;

@Mapper
public interface FollowupPlanMapper extends BaseMapper<FollowupPlan> {
    @Select("SELECT * FROM followup_plan WHERE id = #{id} FOR UPDATE")
    FollowupPlan lock(@Param("id") Long id);
}

