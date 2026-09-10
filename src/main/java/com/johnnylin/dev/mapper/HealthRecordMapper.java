package com.johnnylin.dev.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.johnnylin.dev.domain.HealthRecord;
import org.apache.ibatis.annotations.*;

/**
 * 长者健康体征检测记录持久层数据访问接口。
 *
 * <p>继承 MyBatis-Plus-Join 的 {@link MPJBaseMapper}，提供血压、心率、血氧、体温等检测数据的持久化、多表连表查询与行级排他锁。
 */
@Mapper
public interface HealthRecordMapper extends MPJBaseMapper<HealthRecord> {

    /**
     * 根据主键 ID 使用悲观排他锁锁定健康检测记录。
     *
     * <p>执行 SQL {@code SELECT * FROM health_record WHERE id = #{id} FOR UPDATE}。
     *
     * @param id 健康检测记录主键 ID
     * @return 锁定后的 HealthRecord 实体对象
     */
    @Select("SELECT * FROM health_record WHERE id = #{id} FOR UPDATE")
    HealthRecord lock(@Param("id") Long id);
}

