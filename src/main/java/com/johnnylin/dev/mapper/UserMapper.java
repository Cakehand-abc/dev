package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.User;
import org.apache.ibatis.annotations.*;

/**
 * 系统用户持久层数据访问接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，提供管理员与业务操作员的增删改查及行级排他锁。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户 ID 使用悲观排他锁锁定该系统用户记录。
     *
     * <p>执行 SQL {@code SELECT * FROM sys_user WHERE id = #{id} FOR UPDATE}，
     * 用于修改用户角色、启停状态、重置密码及管理员保底防降权时的并发互斥控制。
     *
     * @param id 用户主键 ID
     * @return 锁定后的 User 实体对象
     */
    @Select("SELECT * FROM sys_user WHERE id = #{id} FOR UPDATE")
    User lock(@Param("id") Long id);
}

