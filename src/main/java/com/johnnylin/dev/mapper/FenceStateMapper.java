package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.FenceState;
import org.apache.ibatis.annotations.*;

/**
 * 电子围栏实时监控状态持久层数据访问接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，提供围栏状态机缓存的维护与行级排他锁查询。
 */
@Mapper
public interface FenceStateMapper extends BaseMapper<FenceState> {

    /**
     * 根据主键 ID 使用悲观排他锁锁定围栏监控状态记录。
     *
     * <p>执行 SQL {@code SELECT * FROM geofence_state WHERE id = #{id} FOR UPDATE}，
     * 用于处理连续定位点流式判定时对状态机更新的加锁保护。
     *
     * @param id 围栏状态记录主键 ID
     * @return 锁定后的 FenceState 实体对象
     */
    @Select("SELECT * FROM geofence_state WHERE id = #{id} FOR UPDATE")
    FenceState lock(@Param("id") Long id);
}

