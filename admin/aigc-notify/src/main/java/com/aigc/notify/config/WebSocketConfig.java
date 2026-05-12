package com.aigc.notify.config;

import com.aigc.common.constant.CommonConstants;
import com.aigc.common.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Map;

/**
 * WebSocket + STOMP 配置
 * 前端通过 SockJS 连接到 /ws/notify，使用 STOMP 协议进行消息订阅和接收
 *
 * 鉴权流程：
 * 1. 握手阶段（beforeHandshake）：验证 URL 参数或请求头中的 JWT Token
 * 2. STOMP CONNECT 阶段（configureClientInboundChannel）：将 userId 设为 STOMP Principal
 * 3. 消息推送阶段：NotifyPushService 通过 convertAndSendToUser(userId, ...) 定向推送
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /** JWT 签名密钥，用于握手阶段验证 Token */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * 注册 STOMP 端点
     * 前端连接地址：ws://host/ws/notify?token={accessToken}
     * 支持 SockJS 降级（浏览器不支持 WebSocket 时自动降级为长轮询）
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notify")
                .setAllowedOriginPatterns("*") // 开发环境全放行，生产环境限制为前端域名
                .addInterceptors(new HandshakeInterceptor() {
                    /**
                     * WebSocket 握手前拦截：验证 JWT Token 并提取用户信息
                     * 支持两种 Token 传递方式：
                     * 1. URL 参数：?token={accessToken}（推荐，WebSocket 无法自定义请求头）
                     * 2. Authorization 请求头：Bearer {accessToken}（HTTP 升级请求时可用）
                     */
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
                        if (request instanceof ServletServerHttpRequest servletRequest) {
                            // 方式一：从 URL 参数 ?token= 读取
                            String token = servletRequest.getServletRequest().getParameter("token");
                            if (token != null) {
                                Claims claims = JwtUtils.parse(jwtSecret, token);
                                if (claims != null) {
                                    // 将 userId/username 存入 WebSocket Session 属性，供后续 CONNECT 阶段使用
                                    attributes.put("userId", claims.getSubject());
                                    attributes.put("username", claims.get("username", String.class));
                                    return true; // 鉴权通过，允许握手
                                }
                            }
                            // 方式二：从 Authorization Header 读取（HTTP Upgrade 请求时可携带）
                            String headerToken = request.getHeaders().getFirst(CommonConstants.AUTHORIZATION_HEADER);
                            if (headerToken != null && headerToken.startsWith(CommonConstants.TOKEN_PREFIX)) {
                                Claims claims = JwtUtils.parse(jwtSecret, headerToken.substring(CommonConstants.TOKEN_PREFIX.length()));
                                if (claims != null) {
                                    attributes.put("userId", claims.getSubject());
                                    return true;
                                }
                            }
                        }
                        log.warn("WebSocket 握手失败：Token 无效");
                        return false; // 鉴权失败，拒绝握手（HTTP 403）
                    }
                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                               WebSocketHandler wsHandler, Exception exception) {}
                })
                .withSockJS(); // 启用 SockJS 降级支持
    }

    /**
     * 配置消息代理（Broker）
     * - /topic：广播消息（一对多，如系统公告）
     * - /queue：点对点消息（结合 /user 前缀实现定向推送）
     * - /app：客户端发送消息的目标前缀（路由到 @MessageMapping 方法）
     * - /user：点对点推送前缀（/user/{userId}/queue/notify）
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic", "/queue"); // 内存消息代理（生产环境可改为 RabbitMQ）
        registry.setApplicationDestinationPrefixes("/app");  // 客户端主动发消息的前缀
        registry.setUserDestinationPrefix("/user");          // 点对点推送前缀
    }

    /**
     * 配置入站 Channel 拦截器
     * 在 STOMP CONNECT 命令时，将握手阶段存储的 userId 设为 STOMP Principal
     * SimpMessagingTemplate.convertAndSendToUser(userId, ...) 依赖 Principal.getName() 路由消息
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // 从 Session 属性中取出握手阶段存储的 userId，设为 STOMP Principal
                    String userId = (String) accessor.getSessionAttributes().get("userId");
                    if (userId != null) {
                        // 匿名内部类实现 Principal，getName() 返回 userId
                        accessor.setUser(new Principal() {
                            @Override public String getName() { return userId; }
                        });
                        log.info("WebSocket 用户连接: userId={}", userId);
                    }
                }
                return message;
            }
        });
    }
}
