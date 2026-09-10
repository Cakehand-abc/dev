package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.HealthRule;
import org.apache.ibatis.annotations.*;

/**
 * 生理健康判定规则持久层数据访问接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，提供体征告警阈值规则维护与行级排他锁查询。
 */
@Mapper
public interface HealthRuleMapper extends BaseMapper<HealthRule> {

    /**
     * 根据主键 ID 使用悲观排他锁锁定健康规则记录。
     *
     * <p>执行 SQL {@code SELECT * FROM health_rule WHERE id = #{id} FOR UPDATE}。
     *
     * @param id 规则主键 ID
     * @return 锁定后的 HealthRule 实体对象
     */
    @Select("SELECT * FROM health_rule WHERE id = #{id} FOR UPDATE")
    HealthRule lock(@Param("id") Long id);
}

