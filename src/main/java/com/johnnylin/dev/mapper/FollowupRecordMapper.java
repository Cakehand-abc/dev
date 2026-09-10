package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.FollowupRecord;
import org.apache.ibatis.annotations.*;

/**
 * 随访执行记录持久层数据访问接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，提供随访履约记录的持久化与行级排他锁查询。
 */
@Mapper
public interface FollowupRecordMapper extends BaseMapper<FollowupRecord> {

    /**
     * 根据记录 ID 使用悲观排他锁锁定随访执行记录。
     *
     * <p>执行 SQL {@code SELECT * FROM followup_record WHERE id = #{id} FOR UPDATE}。
     *
     * @param id 随访记录主键 ID
     * @return 锁定后的 FollowupRecord 实体对象
     */
    @Select("SELECT * FROM followup_record WHERE id = #{id} FOR UPDATE")
    FollowupRecord lock(@Param("id") Long id);
}

