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

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final NotifyPushService pushService;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void send(SendMessageRequest request) {
        Message msg = new Message();
        BeanUtils.copyProperties(request, msg);
        msg.setIsRead(0);
        messageMapper.insert(msg);
        // WebSocket 实时推送
        pushService.pushToUser(request.getReceiverId(), toDTO(msg));
    }

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

    @Override
    public long countUnread(Long receiverId) {
        return messageMapper.selectCount(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getReceiverId, receiverId)
                        .eq(Message::getIsRead, 0));
    }

    @Override
    public void markRead(Long messageId, Long userId) {
        messageMapper.update(null,
                new LambdaUpdateWrapper<Message>()
                        .set(Message::getIsRead, 1)
                        .eq(Message::getId, messageId)
                        .eq(Message::getReceiverId, userId));
    }

    @Override
    public void markAllRead(Long userId) {
        messageMapper.update(null,
                new LambdaUpdateWrapper<Message>()
                        .set(Message::getIsRead, 1)
                        .eq(Message::getReceiverId, userId)
                        .eq(Message::getIsRead, 0));
    }

    private MessageDTO toDTO(Message msg) {
        return MessageDTO.builder()
                .id(msg.getId())
                .senderId(msg.getSenderId())
                .type(msg.getType())
                .title(msg.getTitle())
                .content(msg.getContent())
                .extra(msg.getExtra())
                .isRead(msg.getIsRead())
                .createTime(msg.getCreateTime() != null ? msg.getCreateTime().format(FMT) : null)
                .build();
    }
}
