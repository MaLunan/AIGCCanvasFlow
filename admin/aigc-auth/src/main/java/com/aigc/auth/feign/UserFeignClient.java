package com.aigc.auth.feign;

import com.aigc.common.model.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * aigc-user 服务的 Feign 客户端
 * 供 aigc-auth 服务调用，通过 Spring Cloud 服务发现（Nacos）找到 aigc-user 实例
 * 调用的是 /user/inner/** 内部接口，不经过 Gateway 的 JWT 鉴权
 *
 * name = "aigc-user"：对应 aigc-user 在 Nacos 注册的服务名（spring.application.name）
 * path = "/user/inner"：接口路径前缀
 */
@FeignClient(name = "aigc-user", path = "/user/inner")
public interface UserFeignClient {

    /**
     * 根据用户名查询用户完整信息（含加密密码）
     * 对应 UserController.loadByUsername()
     * 返回的 Map 包含 id、username、password（BCrypt 哈希）、status、nickname 字段
     * 密码哈希仅供 auth 服务做 BCrypt 校验，不对其他服务暴露
     */
    @GetMapping("/loadByUsername")
    R<Map<String, Object>> loadUserByUsername(@RequestParam("username") String username);
}
