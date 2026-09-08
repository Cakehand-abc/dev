package com.johnnylin.dev.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.johnnylin.dev.domain.HeartbeatEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HeartbeatEventMapper extends BaseMapper<HeartbeatEvent> {}
