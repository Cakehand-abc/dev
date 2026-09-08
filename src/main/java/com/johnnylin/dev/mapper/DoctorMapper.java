package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.Doctor;
import org.apache.ibatis.annotations.*;

@Mapper
public interface DoctorMapper extends BaseMapper<Doctor> {
    @Select("SELECT * FROM doctor WHERE id = #{id} FOR UPDATE")
    Doctor lock(@Param("id") Long id);
}

