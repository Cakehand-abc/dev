package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.Audit;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AuditMapper extends BaseMapper<Audit> {
    @Select("SELECT * FROM audit_log WHERE id = #{id} FOR UPDATE")
    Audit lock(@Param("id") Long id);
}

