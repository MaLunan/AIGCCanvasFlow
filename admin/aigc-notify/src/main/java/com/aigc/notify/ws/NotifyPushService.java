package com.aigc.notify.ws;

import com.aigc.notify.dto.MessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * WebSocket 实时推送服务，基于 Spring STOMP 协议
 * 使用 SimpMessagingTemplate 向指定用户或所有用户推送消息
 * 前端通过 STOMP 客户端订阅对应 topic/queue 接收消息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyPushService {

    // Spring STOMP 消息模板，封装 WebSocket 消息发送逻辑
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 推送消息给指定用户（点对点推送）
     * STOMP 目标：/user/{userId}/queue/notify
     * 前端订阅：stompClient.subscribe('/user/queue/notify', handler)
     * 用户离线时推送静默失败，消息已存入数据库，用户登录后可通过 REST 接口查询
     */
    public void pushToUser(Long userId, MessageDTO message) {
        try {
            // convertAndSendToUser 会自动将 /queue/notify 转换为 /user/{userId}/queue/notify
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(userId), // STOMP 用户 principal（与前端连接时注册的 userId 对应）
                    "/queue/notify",
                    message
            );
            log.info("推送消息给用户 {}: {}", userId, message.getTitle());
        } catch (Exception e) {
            // 推送失败不抛异常（消息已存库，不影响主流程）
            log.error("推送消息失败: userId={}", userId, e);
        }
    }

    /**
     * 广播消息给所有在线用户
     * STOMP 目标：/topic/broadcast
     * 前端订阅：stompClient.subscribe('/topic/broadcast', handler)
     * 用于系统公告、全局通知等场景
     */
    public void broadcast(MessageDTO message) {
        messagingTemplate.convertAndSend("/topic/broadcast", message);
    }
}
