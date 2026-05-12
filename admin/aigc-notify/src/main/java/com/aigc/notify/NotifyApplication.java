package com.aigc.notify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 通知服务启动类
 * 提供：消息发送（存库 + WebSocket 推送）、消息查询、已读状态管理
 * 基于 Spring STOMP + SockJS 实现 WebSocket 实时通知
 * 通过 Nacos 注册，服务名 "aigc-notify"
 */
@SpringBootApplication(scanBasePackages = "com.aigc")
public class NotifyApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotifyApplication.class, args);
    }
}
