package com.aigc.notify.mapper;

import com.aigc.notify.entity.Message;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 消息 Mapper：继承 BaseMapper，提供基础 CRUD
 * 已读状态更新通过 LambdaUpdateWrapper 在 Service 层实现
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
