package com.aigc.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 用户服务启动类
 * 提供：用户注册、查询、分页、删除功能
 * 内部 Feign 接口供 aigc-auth 调用（/user/inner/loadByUsername）
 * 通过 Nacos 注册，服务名 "aigc-user"
 */
@SpringBootApplication(scanBasePackages = "com.aigc")
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
