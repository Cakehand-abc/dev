package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.HeartbeatEvent;
import org.apache.ibatis.annotations.Mapper;

/**
 * 智能设备心跳事件持久层数据访问接口。
 *
 * <p>继承 MyBatis-Plus 的 {@link BaseMapper}，支持设备心跳日志的批量写入与事件去重查询。
 */
@Mapper
public interface HeartbeatEventMapper extends BaseMapper<HeartbeatEvent> {}
