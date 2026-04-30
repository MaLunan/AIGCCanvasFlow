package com.aigc.notify.service;

import com.aigc.notify.dto.MessageDTO;
import com.aigc.notify.dto.SendMessageRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface MessageService {
    void send(SendMessageRequest request);
    Page<MessageDTO> listByReceiver(Long receiverId, int current, int size);
    long countUnread(Long receiverId);
    void markRead(Long messageId, Long userId);
    void markAllRead(Long userId);
}
