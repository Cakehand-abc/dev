package com.johnnylin.dev.mapper;

import com.github.yulichang.base.MPJBaseMapper;
import com.johnnylin.dev.domain.Doctor;
import org.apache.ibatis.annotations.*;

/**
 * 随访医生持久层数据访问接口。
 *
 * <p>继承 MyBatis-Plus-Join 的 {@link MPJBaseMapper}，提供医生档案维护、连表查询与行级排他锁查询。
 */
@Mapper
public interface DoctorMapper extends MPJBaseMapper<Doctor> {

    /**
     * 根据医生 ID 使用悲观排他锁锁定医生档案记录。
     *
     * <p>执行 SQL {@code SELECT * FROM doctor WHERE id = #{id} FOR UPDATE}，
     * 用于医生档案信息修改时的版本校验防并发。
     *
     * @param id 医生主键 ID
     * @return 锁定后的 Doctor 实体对象
     */
    @Select("SELECT * FROM doctor WHERE id = #{id} FOR UPDATE")
    Doctor lock(@Param("id") Long id);
}

