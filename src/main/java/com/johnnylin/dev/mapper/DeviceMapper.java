package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.Device;
import org.apache.ibatis.annotations.*;

/**
 * 智能穿戴手环/腕表设备持久层数据访问接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，提供设备台账维护与行级排他锁查询。
 */
@Mapper
public interface DeviceMapper extends BaseMapper<Device> {

    /**
     * 根据设备 ID 使用悲观排他锁锁定该设备记录。
     *
     * <p>执行 SQL {@code SELECT * FROM watch_device WHERE id = #{id} FOR UPDATE}，
     * 用于设备绑定、解绑、状态更新及心跳接入时的并发同步控制。
     *
     * @param id 设备主键 ID
     * @return 锁定后的 Device 实体对象
     */
    @Select("SELECT * FROM watch_device WHERE id = #{id} FOR UPDATE")
    Device lock(@Param("id") Long id);
}

