package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.Audit;
import org.apache.ibatis.annotations.*;

/**
 * 操作审计日志持久层数据访问接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，支持操作审计流的写入、分页检索与行级排他锁。
 */
@Mapper
public interface AuditMapper extends BaseMapper<Audit> {

    /**
     * 根据主键 ID 使用悲观排他锁锁定审计日志记录。
     *
     * <p>执行 SQL {@code SELECT * FROM audit_log WHERE id = #{id} FOR UPDATE}。
     *
     * @param id 审计日志主键 ID
     * @return 锁定后的 Audit 实体对象
     */
    @Select("SELECT * FROM audit_log WHERE id = #{id} FOR UPDATE")
    Audit lock(@Param("id") Long id);
}

