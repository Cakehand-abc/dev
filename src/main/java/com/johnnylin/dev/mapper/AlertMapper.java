package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.Alert;
import org.apache.ibatis.annotations.*;

/**
 * 电子围栏告警事件持久层数据访问接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，提供告警事件的增删改查及行级悲观排他锁查询。
 */
@Mapper
public interface AlertMapper extends BaseMapper<Alert> {

    /**
     * 根据告警 ID 使用悲观排他锁锁定该条记录。
     *
     * <p>执行 SQL {@code SELECT * FROM geofence_alert WHERE id = #{id} FOR UPDATE}，
     * 用于人工处置或系统自动关闭告警时的并发冲突保护。
     *
     * @param id 告警记录主键 ID
     * @return 锁定后的 Alert 实体对象；若不存在则返回 null
     */
    @Select("SELECT * FROM geofence_alert WHERE id = #{id} FOR UPDATE")
    Alert lock(@Param("id") Long id);
}

