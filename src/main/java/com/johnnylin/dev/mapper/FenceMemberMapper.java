package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.FenceMember;
import org.apache.ibatis.annotations.*;

/**
 * 电子围栏成员关联持久层数据访问接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，提供围栏纳管人员关联的维护与行级排他锁查询。
 */
@Mapper
public interface FenceMemberMapper extends BaseMapper<FenceMember> {

    /**
     * 根据主键 ID 使用悲观排他锁锁定围栏成员记录。
     *
     * <p>执行 SQL {@code SELECT * FROM geofence_member WHERE id = #{id} FOR UPDATE}。
     *
     * @param id 围栏成员关联主键 ID
     * @return 锁定后的 FenceMember 实体对象
     */
    @Select("SELECT * FROM geofence_member WHERE id = #{id} FOR UPDATE")
    FenceMember lock(@Param("id") Long id);
}

