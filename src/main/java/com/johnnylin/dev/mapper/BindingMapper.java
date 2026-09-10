package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.Binding;
import org.apache.ibatis.annotations.*;

/**
 * 智能设备与长者绑定关系持久层数据访问接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，提供设备绑定的生命周期维护与行级排他锁查询。
 */
@Mapper
public interface BindingMapper extends BaseMapper<Binding> {

    /**
     * 根据主键 ID 使用悲观排他锁锁定该条绑定记录。
     *
     * <p>执行 SQL {@code SELECT * FROM device_binding WHERE id = #{id} FOR UPDATE}，
     * 用于解绑或状态流转时的并发安全控制。
     *
     * @param id 绑定记录主键 ID
     * @return 锁定后的 Binding 实体对象
     */
    @Select("SELECT * FROM device_binding WHERE id = #{id} FOR UPDATE")
    Binding lock(@Param("id") Long id);
}

