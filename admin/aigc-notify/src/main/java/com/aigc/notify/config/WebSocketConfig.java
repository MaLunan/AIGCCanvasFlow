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

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/notify")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
                        if (request instanceof ServletServerHttpRequest servletRequest) {
                            String token = servletRequest.getServletRequest().getParameter("token");
                            if (token != null) {
                                Claims claims = JwtUtils.parse(jwtSecret, token);
                                if (claims != null) {
                                    attributes.put("userId", claims.getSubject());
                                    attributes.put("username", claims.get("username", String.class));
                                    return true;
                                }
                            }
                            // 也支持 Header 方式
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
                        return false;
                    }
                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                               WebSocketHandler wsHandler, Exception exception) {}
                })
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 服务端推送前缀：/topic（广播）、/queue（点对点）
        registry.enableSimpleBroker("/topic", "/queue");
        // 客户端发送消息前缀
        registry.setApplicationDestinationPrefixes("/app");
        // 点对点消息前缀（结合 /queue）
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String userId = (String) accessor.getSessionAttributes().get("userId");
                    if (userId != null) {
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
