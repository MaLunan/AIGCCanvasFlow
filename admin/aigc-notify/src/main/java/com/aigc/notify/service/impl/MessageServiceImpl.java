package com.aigc.notify.service.impl;

import com.aigc.notify.dto.MessageDTO;
import com.aigc.notify.dto.SendMessageRequest;
import com.aigc.notify.entity.Message;
import com.aigc.notify.mapper.MessageMapper;
import com.aigc.notify.service.MessageService;
import com.aigc.notify.ws.NotifyPushService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * 消息服务实现：消息发送（存库 + WebSocket 实时推送）、已读状态管理
 * 消息发送采用"存库 + 即时推送"双写模式：
 * - 先写数据库（持久化，用于历史消息查询）
 * - 再通过 WebSocket 推送（实时通知，用户在线时即时收到）
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    // WebSocket 推送服务，基于 Spring STOMP 协议
    private final NotifyPushService pushService;

    /** 时间格式化器，将 LocalDateTime 格式化为前端友好的字符串 */
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 发送消息：持久化到数据库 + WebSocket 实时推送给接收者
     * 即使 WebSocket 推送失败（用户离线），消息也已写入数据库，用户登录后可查看
     */
    @Override
    public void send(SendMessageRequest request) {
        // 创建消息实体，初始状态为未读
        Message msg = new Message();
        BeanUtils.copyProperties(request, msg);
        msg.setIsRead(0);  // 0=未读，消息发送时默认未读
        messageMapper.insert(msg);
        // 尝试通过 WebSocket 实时推送给在线用户（用户离线时推送静默失败）
        pushService.pushToUser(request.getReceiverId(), toDTO(msg));
    }

    /**
     * 分页查询用户收到的消息列表（按 createTime 倒序，最新消息在前）
     */
    @Override
    public Page<MessageDTO> listByReceiver(Long receiverId, int current, int size) {
        Page<Message> page = messageMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getReceiverId, receiverId)
                        .orderByDesc(Message::getCreateTime));
        Page<MessageDTO> dtoPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        dtoPage.setRecords(page.getRecords().stream().map(this::toDTO).toList());
        return dtoPage;
    }

    /** 统计用户未读消息数量（isRead=0 的记录数） */
    @Override
    public long countUnread(Long receiverId) {
        return messageMapper.selectCount(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getReceiverId, receiverId)
                        .eq(Message::getIsRead, 0)); // 0=未读
    }

    /**
     * 标记单条消息已读
     * 双重条件（id + receiverId）防止越权：用户只能标记自己收到的消息
     */
    @Override
    public void markRead(Long messageId, Long userId) {
        messageMapper.update(null,
                new LambdaUpdateWrapper<Message>()
                        .set(Message::getIsRead, 1)           // 设为已读
                        .eq(Message::getId, messageId)
                        .eq(Message::getReceiverId, userId));  // 校验归属
    }

    /**
     * 全部标记已读（批量更新当前用户的所有未读消息）
     * 只更新 isRead=0 的记录，避免不必要的全表扫描
     */
    @Override
    public void markAllRead(Long userId) {
        messageMapper.update(null,
                new LambdaUpdateWrapper<Message>()
                        .set(Message::getIsRead, 1)
                        .eq(Message::getReceiverId, userId)
                        .eq(Message::getIsRead, 0));  // 只更新未读消息
    }

    /** Message Entity → MessageDTO，时间格式化为字符串 */
    private MessageDTO toDTO(Message msg) {
        return MessageDTO.builder()
                .id(msg.getId())
                .senderId(msg.getSenderId())
                .type(msg.getType())
                .title(msg.getTitle())
                .content(msg.getContent())
                .extra(msg.getExtra())   // 扩展信息（如关联的项目 ID、节点 ID）
                .isRead(msg.getIsRead())
                // LocalDateTime → 格式化字符串（前端直接显示）
                .createTime(msg.getCreateTime() != null ? msg.getCreateTime().format(FMT) : null)
                .build();
    }
}
