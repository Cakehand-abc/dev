package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.Region;
import org.apache.ibatis.annotations.*;

/**
 * 社区行政网格片区持久层数据访问接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，提供网格片区基础数据的维护与行级排他锁查询。
 */
@Mapper
public interface RegionMapper extends BaseMapper<Region> {

    /**
     * 根据片区 ID 使用悲观排他锁锁定该网格片区记录。
     *
     * <p>执行 SQL {@code SELECT * FROM region WHERE id = #{id} FOR UPDATE}。
     *
     * @param id 片区主键 ID
     * @return 锁定后的 Region 实体对象
     */
    @Select("SELECT * FROM region WHERE id = #{id} FOR UPDATE")
    Region lock(@Param("id") Long id);
}

