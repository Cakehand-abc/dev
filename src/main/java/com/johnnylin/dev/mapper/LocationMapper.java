package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.Location;
import org.apache.ibatis.annotations.*;

/**
 * GPS 定位轨迹点持久层数据访问接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，提供轨迹流式入库、按时间段回放检索与行级排他锁查询。
 */
@Mapper
public interface LocationMapper extends BaseMapper<Location> {

    /**
     * 根据定位点主键 ID 使用悲观排他锁锁定该轨迹点。
     *
     * <p>执行 SQL {@code SELECT * FROM location_point WHERE id = #{id} FOR UPDATE}。
     *
     * @param id 轨迹点主键 ID
     * @return 锁定后的 Location 实体对象
     */
    @Select("SELECT * FROM location_point WHERE id = #{id} FOR UPDATE")
    Location lock(@Param("id") Long id);
}

