package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.Fence;
import org.apache.ibatis.annotations.*;

/**
 * 电子地理围栏持久层数据访问接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，提供围栏区域参数的维护与行级排他锁查询。
 */
@Mapper
public interface FenceMapper extends BaseMapper<Fence> {

    /**
     * 根据围栏 ID 使用悲观排他锁锁定该围栏定义。
     *
     * <p>执行 SQL {@code SELECT * FROM geofence WHERE id = #{id} FOR UPDATE}，
     * 用于围栏几何范围重新配置或成员变更时的防并发互斥。
     *
     * @param id 围栏主键 ID
     * @return 锁定后的 Fence 实体对象
     */
    @Select("SELECT * FROM geofence WHERE id = #{id} FOR UPDATE")
    Fence lock(@Param("id") Long id);
}

