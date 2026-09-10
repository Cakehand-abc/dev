package com.johnnylin.dev.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.johnnylin.dev.domain.Elder;
import org.apache.ibatis.annotations.*;

/**
 * 长者健康档案持久层数据访问接口。
 *
 * <p>继承 MyBatis-Plus-Join 的 {@link MPJBaseMapper}，提供长者档案的增删改查、多表连表查询及悲观排他锁。
 */
@Mapper
public interface ElderMapper extends MPJBaseMapper<Elder> {

    /**
     * 根据长者 ID 使用悲观排他锁锁定档案记录。
     *
     * <p>执行 SQL {@code SELECT * FROM elder WHERE id = #{id} FOR UPDATE}，
     * 用于修改档案、归档迁出、删除前关联检查或设备绑定时的防并发互斥控制。
     *
     * @param id 长者档案主键 ID
     * @return 锁定后的 Elder 实体对象
     */
    @Select("SELECT * FROM elder WHERE id = #{id} FOR UPDATE")
    Elder lock(@Param("id") Long id);
}

