package com.aigc.notify.service;

import com.aigc.notify.dto.MessageDTO;
import com.aigc.notify.dto.SendMessageRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

/**
 * 消息服务接口：站内消息的发送、查询和已读管理
 * 发送时采用"存库 + 实时 STOMP 推送"双写策略，接收方不在线时消息仅存库
 * 实现类：MessageServiceImpl
 */
public interface MessageService {
    /** 发送消息（存库 + STOMP 实时推送），senderId=null 表示系统消息 */
    void send(SendMessageRequest request);

    /** 分页查询指定用户的消息收件箱（按发送时间倒序） */
    Page<MessageDTO> listByReceiver(Long receiverId, int current, int size);

    /** 统计未读消息数量（用于前端角标显示） */
    long countUnread(Long receiverId);

    /** 将指定消息标记为已读（双条件校验：id + receiverId，防止越权） */
    void markRead(Long messageId, Long userId);

    /** 将当前用户所有未读消息批量标记为已读 */
    void markAllRead(Long userId);
}
