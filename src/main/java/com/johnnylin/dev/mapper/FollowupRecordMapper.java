package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.FollowupRecord;
import org.apache.ibatis.annotations.*;

@Mapper
public interface FollowupRecordMapper extends BaseMapper<FollowupRecord> {
    @Select("SELECT * FROM followup_record WHERE id = #{id} FOR UPDATE")
    FollowupRecord lock(@Param("id") Long id);
}

