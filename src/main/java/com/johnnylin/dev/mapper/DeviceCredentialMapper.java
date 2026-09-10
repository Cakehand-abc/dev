package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.DeviceCredential;
import org.apache.ibatis.annotations.Mapper;

/**
 * 设备安全接入凭据持久层数据访问接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，提供设备密钥哈希的增删改查。
 */
@Mapper
public interface DeviceCredentialMapper extends BaseMapper<DeviceCredential> {}
