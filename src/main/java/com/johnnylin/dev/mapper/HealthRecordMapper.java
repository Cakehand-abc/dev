package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.HealthRecord;
import org.apache.ibatis.annotations.*;

@Mapper
public interface HealthRecordMapper extends BaseMapper<HealthRecord> {
    @Select("SELECT * FROM health_record WHERE id = #{id} FOR UPDATE")
    HealthRecord lock(@Param("id") Long id);
}

