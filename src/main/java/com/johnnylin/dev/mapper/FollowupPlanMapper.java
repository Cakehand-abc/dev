package com.johnnylin.dev.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.johnnylin.dev.domain.FollowupPlan;
import org.apache.ibatis.annotations.*;

/**
 * 医生随访计划持久层数据访问接口。
 *
 * <p>继承 MyBatis-Plus-Join 的 {@link MPJBaseMapper}，提供随访任务的创建、状态更新、多表连表聚合与行级排他锁查询。
 */
@Mapper
public interface FollowupPlanMapper extends MPJBaseMapper<FollowupPlan> {

    /**
     * 根据计划 ID 使用悲观排他锁锁定随访计划记录。
     *
     * <p>执行 SQL {@code SELECT * FROM followup_plan WHERE id = #{id} FOR UPDATE}，
     * 保证在完成随访或取消随访时严格防并发重复提交。
     *
     * @param id 随访计划主键 ID
     * @return 锁定后的 FollowupPlan 实体对象
     */
    @Select("SELECT * FROM followup_plan WHERE id = #{id} FOR UPDATE")
    FollowupPlan lock(@Param("id") Long id);
}

