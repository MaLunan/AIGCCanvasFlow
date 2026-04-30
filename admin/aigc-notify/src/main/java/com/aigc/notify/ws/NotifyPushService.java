package com.aigc.notify.ws;

import com.aigc.notify.dto.MessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyPushService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 推送消息给指定用户（STOMP /user/{userId}/queue/notify）
     */
    public void pushToUser(Long userId, MessageDTO message) {
        try {
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId),
                    "/queue/notify",
                    message
            );
            log.info("推送消息给用户 {}: {}", userId, message.getTitle());
        } catch (Exception e) {
            log.error("推送消息失败: userId={}", userId, e);
        }
    }

    /**
     * 广播消息（STOMP /topic/broadcast）
     */
    public void broadcast(MessageDTO message) {
        messagingTemplate.convertAndSend("/topic/broadcast", message);
    }
}
